#!/bin/bash

# SEMP API configuration
SEMP_URL="http://localhost:8080/SEMP/v2/config"
MONITOR_URL="http://localhost:8080/SEMP/v2/monitor"
AUTH="admin:admin"
VPN="default"
MOCK_IDP_URL="http://localhost:9090"

# Wait for the broker's message VPN to become operationally "up" before provisioning and testing.
# SEMP (config plane) responds well before guaranteed messaging (message spool) is ready, so a fixed
# sleep is not enough: queue/durable/transacted tests fail intermittently if they run while the VPN
# state is still "down". Poll the monitor API for state == "up".
echo "Waiting for Solace message VPN '$VPN' to become operational..."
vpn_up=false
for i in $(seq 1 60); do
    state=$(curl -s -u "$AUTH" "$MONITOR_URL/msgVpns/$VPN?select=state" 2>/dev/null \
        | grep -o '"state"[[:space:]]*:[[:space:]]*"[^"]*"' | grep -o '[^"]*"$' | tr -d '"')
    if [ "$state" = "up" ]; then
        echo "Message VPN is up after ~$((i * 3))s"
        vpn_up=true
        break
    fi
    sleep 3
done

if [ "$vpn_up" != "true" ]; then
    echo "WARNING: Message VPN did not report 'up' within the timeout; proceeding anyway."
fi

# Settle margin after the VPN reports up. The transaction subsystem needs a little longer than plain
# guaranteed messaging to stabilize, so give it extra headroom to avoid flaky transacted tests.
sleep 10

# Function to create a queue
create_queue() {
    local queue_name=$1
    echo "Creating queue: $queue_name"

    for attempt in 1 2 3 4 5; do
        response=$(curl -s -X POST "$SEMP_URL/msgVpns/$VPN/queues" \
            -u "$AUTH" \
            -H "Content-Type: application/json" \
            -d "{
                \"queueName\": \"$queue_name\",
                \"accessType\": \"exclusive\",
                \"permission\": \"delete\",
                \"ingressEnabled\": true,
                \"egressEnabled\": true,
                \"respectTtlEnabled\": true
            }")
        if ! echo "$response" | grep -q '"error"'; then
            break
        fi
        sleep 3
    done

    # Subscribe queue to topic with same name (for topic-to-queue mapping). The queue name is a
    # URL path segment here (unlike the JSON body above), so literal '/' characters must be
    # percent-encoded or SEMP splits it into multiple path segments and rejects it as INVALID_PATH.
    local queue_name_encoded
    queue_name_encoded=$(printf '%s' "$queue_name" | sed 's#/#%2F#g')
    curl -X POST "$SEMP_URL/msgVpns/$VPN/queues/$queue_name_encoded/subscriptions" \
        -u "$AUTH" \
        -H "Content-Type: application/json" \
        -d "{
            \"subscriptionTopic\": \"$queue_name\"
        }" \
        2>/dev/null
}

# Function to create an OAuth2/OIDC authentication profile against the mock IdP.
# role: "resource-server" (OAuth2 access-token auth) or "client" (OIDC ID-token auth).
create_oauth_profile() {
    local profile_name=$1 role=$2
    echo "Creating OAuth profile: $profile_name (role: $role)"

    curl -X POST "$SEMP_URL/msgVpns/$VPN/authenticationOauthProfiles" \
        -u "$AUTH" \
        -H "Content-Type: application/json" \
        -d "{
            \"oauthProfileName\": \"$profile_name\",
            \"oauthRole\": \"$role\",
            \"issuer\": \"http://mock-idp:8080/default\",
            \"endpointJwks\": \"http://mock-idp:8080/default/jwks\",
            \"clientId\": \"solace\",
            \"clientRequiredType\": \"JWT\",
            \"clientValidateTypeEnabled\": true,
            \"resourceServerRequiredIssuer\": \"http://mock-idp:8080/default\",
            \"resourceServerValidateIssuerEnabled\": true,
            \"resourceServerRequiredAudience\": \"solace\",
            \"resourceServerValidateAudienceEnabled\": true,
            \"resourceServerValidateScopeEnabled\": false,
            \"resourceServerRequiredType\": \"JWT\",
            \"resourceServerValidateTypeEnabled\": true,
            \"enabled\": true
        }" \
        2>/dev/null
}

# Producer test queues
echo "Creating producer test queues..."
create_queue "test/producer/init/queue"
create_queue "test/producer/text/queue"
create_queue "test/producer/binary/queue"
create_queue "test/producer/properties/queue"
create_queue "test/producer/metadata/queue"
create_queue "test/producer/ttl/queue"
create_queue "test/producer/persistent/queue"
create_queue "test/producer/userdata/queue"
create_queue "test/producer/compression/queue"

# Producer transaction test queues
echo "Creating producer transaction test queues..."
create_queue "test/producer/tx/commit/queue"
create_queue "test/producer/tx/rollback/queue"
create_queue "test/producer/tx/multiple/queue"

# Consumer test queues
echo "Creating consumer test queues..."
create_queue "test/consumer/init/queue"
create_queue "test/consumer/text/queue"
create_queue "test/consumer/binary/queue"
create_queue "test/consumer/properties/queue"
create_queue "test/consumer/metadata/queue"
create_queue "test/consumer/timeout/queue"
create_queue "test/consumer/expiration/queue"
create_queue "test/consumer/nowait/queue"
create_queue "test/consumer/selector/queue"
create_queue "test/consumer/multiple/queue"
create_queue "test/consumer/flow/queue"

# Consumer transaction test queues
echo "Creating consumer transaction test queues..."
create_queue "test/consumer/tx/commit/queue"
create_queue "test/consumer/tx/rollback/queue"
create_queue "test/consumer/tx/multiple/queue"
create_queue "test/consumer/tx/mixed/queue"
create_queue "test/consumer/tx/coordinated/queue"

# Client ACK test queues
echo "Creating client ACK test queues..."
create_queue "test/consumer/ack/single/queue"
create_queue "test/consumer/ack/multiple/queue"
create_queue "test/consumer/nack/requeue/queue"
create_queue "test/consumer/nack/reject/queue"
create_queue "test/consumer/ack/redelivery/queue"
create_queue "test/consumer/ack/defaultmode/queue"

# Auth test queues
echo "Creating auth test queues..."
create_queue "test/consumer/auth/basic/queue"
create_queue "test/consumer/auth/tls/queue"
create_queue "test/consumer/auth/oauth/access/queue"
create_queue "test/consumer/auth/oauth/oidc/queue"

# OAuth2/OIDC setup: enable the auth scheme on the VPN, create an authorization group matching
# the "groups" claim the mock IdP injects into tokens issued for scope=solace (see
# docker-compose.yaml's mock-idp JSON_CONFIG), and create one profile per role.
echo "Configuring OAuth2/OIDC authentication..."
curl -X PATCH "$SEMP_URL/msgVpns/$VPN" \
    -u "$AUTH" \
    -H "Content-Type: application/json" \
    -d '{"authenticationOauthEnabled": true}' \
    2>/dev/null

curl -X POST "$SEMP_URL/msgVpns/$VPN/authorizationGroups" \
    -u "$AUTH" \
    -H "Content-Type: application/json" \
    -d '{"authorizationGroupName": "solace-group", "enabled": true}' \
    2>/dev/null

create_oauth_profile "test_access_profile" "resource-server"
create_oauth_profile "test_oidc_profile" "client"

# Listener test queues
echo "Creating listener test queues..."
create_queue "test/listener/autoack/queue"
create_queue "test/listener/clientack/queue"
create_queue "test/listener/nack/queue"
create_queue "test/listener/tx/commit/queue"
create_queue "test/listener/tx/rollback/queue"

# Error test queues
echo "Creating error test queues..."
create_queue "test/error/empty/payload/queue"
create_queue "test/error/large/payload/queue"
create_queue "test/error/special/chars/queue"

# REST consumer test queues
echo "Creating REST consumer test queues..."
create_queue "jsonQueue"
create_queue "textQueue"
create_queue "xmlQueue"
create_queue "binaryQueue"
create_queue "invalidJsonQueue"
create_queue "emptyRestQueue"

# Data-binding test queues. Positive (successful-binding) cases share one queue per test file;
# negative (mismatch) cases each keep their own dedicated queue (see test_config.bal for rationale).
echo "Creating data-binding test queues..."
create_queue "test/binding/validation/queue"
create_queue "test/binding/service/positive/queue"
create_queue "test/binding/service/mismatch/string/queue"
create_queue "test/binding/service/mismatch/int/queue"
create_queue "test/binding/service/mismatch/record/queue"
create_queue "test/binding/service/noonerror/queue"
create_queue "test/binding/client/positive/queue"
create_queue "test/binding/client/mismatch/string/queue"
create_queue "test/binding/client/mismatch/int/queue"
create_queue "test/binding/client/mismatch/record/queue"
create_queue "test/binding/producer/queue"

echo "Queue creation completed!"
echo "Solace broker is ready for testing."
