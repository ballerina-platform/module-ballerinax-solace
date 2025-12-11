#!/bin/bash

# Wait for Solace broker to start up
echo "Waiting for Solace broker to start..."
sleep 15

# SEMP API configuration
SEMP_URL="http://localhost:8080/SEMP/v2/config"
AUTH="admin:admin"
VPN="default"

# Function to create a queue
create_queue() {
    local queue_name=$1
    echo "Creating queue: $queue_name"

    curl -X POST "$SEMP_URL/msgVpns/$VPN/queues" \
        -u "$AUTH" \
        -H "Content-Type: application/json" \
        -d "{
            \"queueName\": \"$queue_name\",
            \"accessType\": \"exclusive\",
            \"permission\": \"delete\",
            \"ingressEnabled\": true,
            \"egressEnabled\": true,
            \"respectTtlEnabled\": true
        }" \
        2>/dev/null

    # Subscribe queue to topic with same name (for topic-to-queue mapping)
    curl -X POST "$SEMP_URL/msgVpns/$VPN/queues/$queue_name/subscriptions" \
        -u "$AUTH" \
        -H "Content-Type: application/json" \
        -d "{
            \"subscriptionTopic\": \"$queue_name\"
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

# Error test queues
echo "Creating error test queues..."
create_queue "test/error/empty/payload/queue"
create_queue "test/error/large/payload/queue"
create_queue "test/error/special/chars/queue"

echo "Queue creation completed!"
echo "Solace broker is ready for testing."
