#!/bin/bash
# Script to initialize Solace queues and SSL configuration for testing using SEMP API
# Configures queues with guaranteed messaging support for transacted sessions
# Enables SSL/TLS secure transport

set -e

echo "Waiting for Solace broker to be ready..."
sleep 15

SOLACE_URL="http://localhost:8080"
SEMP_URL="${SOLACE_URL}/SEMP/v2/config"
VPN="default"
AUTH="admin:admin"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CERTS_DIR="${SCRIPT_DIR}/certs"

# Function to create a queue with guaranteed messaging support
create_queue() {
    local queue_name=$1
    echo "Creating queue: $queue_name"

    curl -X POST "${SEMP_URL}/msgVpns/${VPN}/queues" \
        -u "${AUTH}" \
        -H "Content-Type: application/json" \
        -d "{
            \"queueName\": \"${queue_name}\",
            \"accessType\": \"exclusive\",
            \"permission\": \"delete\",
            \"ingressEnabled\": true,
            \"egressEnabled\": true,
            \"maxMsgSpoolUsage\": 100,
            \"respectTtlEnabled\": true
        }" \
        -s -o /dev/null -w "%{http_code}\n" || true
}

# Note: Compression is enabled by default on the Solace broker on port 55003
# The SMF service compression is already available, no additional configuration needed

# Note: Queues are configured for guaranteed messaging (persistent delivery)
# This enables support for transacted sessions which require guaranteed transport

# Function to configure SSL/TLS on the message VPN
configure_ssl() {
    echo "Configuring SSL/TLS for message VPN..."

    # Enable SSL on the message VPN
    curl -X PATCH "${SEMP_URL}/msgVpns/${VPN}" \
        -u "${AUTH}" \
        -H "Content-Type: application/json" \
        -d '{
            "serviceSmfPlainTextEnabled": true,
            "serviceSmfCompressionEnabled": true,
            "serviceSmfTlsEnabled": true
        }' \
        -s -o /dev/null -w "%{http_code}\n" || true

    echo "SSL/TLS configuration completed (note: actual SSL requires broker restart with certificates)"
}

# Create queues for basic tests
create_queue "test-queue"
create_queue "test-transacted-queue"

# Create queues for SSL/TLS tests
create_queue "ssl-test-queue"
create_queue "ssl-producer-text-queue"
create_queue "ssl-producer-bytes-queue"
create_queue "ssl-producer-map-queue"
create_queue "ssl-consumer-queue"
create_queue "ssl-transacted-queue"
create_queue "ssl-client-cert-queue"

# Configure SSL/TLS
configure_ssl

echo ""
echo "================================================"
echo "Solace initialization completed!"
echo "================================================"
echo "Created queues:"
echo "  - Basic test queues: test-queue, test-transacted-queue"
echo "  - SSL test queues: ssl-test-queue, ssl-producer-*, ssl-consumer-queue, etc."
echo "SSL/TLS enabled on message VPN: ${VPN}"
echo ""
echo "Note: For SSL/TLS to work, server certificates must be configured via CLI or GUI"
echo "================================================"

