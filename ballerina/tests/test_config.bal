// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

// Broker connection configuration
const string BROKER_URL = "tcp://localhost:55554";
const string BROKER_URL_COMPRESSED = "tcp://localhost:55003";
const string MESSAGE_VPN = "default";
const string BROKER_USERNAME = "admin";
const string BROKER_PASSWORD = "admin";

// Test message content
const string TEXT_MESSAGE_CONTENT = "Hello Solace SMF!";

// Producer test queues
const string PRODUCER_INIT_QUEUE = "test/producer/init/queue";
const string PRODUCER_TEXT_QUEUE = "test/producer/text/queue";
const string PRODUCER_BINARY_QUEUE = "test/producer/binary/queue";
const string PRODUCER_PROPERTIES_QUEUE = "test/producer/properties/queue";
const string PRODUCER_METADATA_QUEUE = "test/producer/metadata/queue";
const string PRODUCER_TTL_QUEUE = "test/producer/ttl/queue";
const string PRODUCER_PERSISTENT_QUEUE = "test/producer/persistent/queue";
const string PRODUCER_USERDATA_QUEUE = "test/producer/userdata/queue";
const string PRODUCER_COMPRESSION_QUEUE = "test/producer/compression/queue";

// Producer transaction test queues
const string PRODUCER_TX_COMMIT_QUEUE = "test/producer/tx/commit/queue";
const string PRODUCER_TX_ROLLBACK_QUEUE = "test/producer/tx/rollback/queue";
const string PRODUCER_TX_MULTIPLE_QUEUE = "test/producer/tx/multiple/queue";

// Producer test topics
const string PRODUCER_TOPIC = "test/producer/topic";

// Consumer test queues
const string CONSUMER_INIT_QUEUE = "test/consumer/init/queue";
const string CONSUMER_TEXT_QUEUE = "test/consumer/text/queue";
const string CONSUMER_BINARY_QUEUE = "test/consumer/binary/queue";
const string CONSUMER_PROPERTIES_QUEUE = "test/consumer/properties/queue";
const string CONSUMER_METADATA_QUEUE = "test/consumer/metadata/queue";
const string CONSUMER_TIMEOUT_QUEUE = "test/consumer/timeout/queue";
const string CONSUMER_NOWAIT_QUEUE = "test/consumer/nowait/queue";
const string CONSUMER_SELECTOR_QUEUE = "test/consumer/selector/queue";
const string CONSUMER_MULTIPLE_QUEUE = "test/consumer/multiple/queue";
const string CONSUMER_FLOW_QUEUE = "test/consumer/flow/queue";

// Consumer transaction test queues
const string CONSUMER_TX_COMMIT_QUEUE = "test/consumer/tx/commit/queue";
const string CONSUMER_TX_ROLLBACK_QUEUE = "test/consumer/tx/rollback/queue";
const string CONSUMER_TX_MULTIPLE_QUEUE = "test/consumer/tx/multiple/queue";
const string CONSUMER_TX_MIXED_QUEUE = "test/consumer/tx/mixed/queue";
const string CONSUMER_TX_COORDINATED_QUEUE = "test/consumer/tx/coordinated/queue";

// Consumer test topics
const string CONSUMER_DIRECT_TOPIC = "test/consumer/direct/topic";
const string CONSUMER_DURABLE_TOPIC = "test/consumer/durable/topic";
const string CONSUMER_DURABLE_ENDPOINT = "test-consumer-durable-endpoint";

// Client ACK test queues
const string ACK_SINGLE_QUEUE = "test/consumer/ack/single/queue";
const string ACK_MULTIPLE_QUEUE = "test/consumer/ack/multiple/queue";
const string NACK_REQUEUE_QUEUE = "test/consumer/nack/requeue/queue";
const string NACK_REJECT_QUEUE = "test/consumer/nack/reject/queue";
const string ACK_REDELIVERY_QUEUE = "test/consumer/ack/redelivery/queue";

// Error test queues
const string ERROR_EMPTY_PAYLOAD_QUEUE = "test/error/empty/payload/queue";
const string ERROR_LARGE_PAYLOAD_QUEUE = "test/error/large/payload/queue";
const string ERROR_SPECIAL_CHARS_QUEUE = "test/error/special/chars/queue";

// Test timeouts
const decimal DEFAULT_RECEIVE_TIMEOUT = 5.0;
const decimal SHORT_RECEIVE_TIMEOUT = 1.0;
const decimal NO_WAIT_TIMEOUT = 0.0;
