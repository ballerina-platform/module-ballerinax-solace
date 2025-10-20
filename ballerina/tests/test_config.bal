// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com).
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

// Solace broker connection details
const string BROKER_URL = "smf://localhost:55554";
const string BROKER_URL_COMPRESSED = "smf://localhost:55003";
const string BROKER_URL_SSL = "smfs://localhost:55443";
const string MESSAGE_VPN = "default";
const string BROKER_USERNAME = "admin";
const string BROKER_PASSWORD = "admin";

// SSL/TLS certificate paths (relative to test directory)
const string TRUSTSTORE_PATH = "tests/resources/certs/truststore.jks";
const string TRUSTSTORE_PASSWORD = "changeit";
const string KEYSTORE_PATH = "tests/resources/certs/client-keystore.jks";
const string KEYSTORE_PASSWORD = "changeit";
const string KEYSTORE_KEY_PASSWORD = "changeit";

// Test queue and topic names - each test uses a unique queue to avoid interference
const string TEST_QUEUE = "test-queue";
const string TEST_TOPIC = "test/topic";
const string TEST_TRANSACTED_QUEUE = "test-transacted-queue";

// Consumer test queues
const string CONSUMER_INIT_QUEUE = "consumer-init-queue";
const string CONSUMER_RECEIVE_QUEUE = "consumer-receive-queue";
const string CONSUMER_RECEIVE_NO_WAIT_QUEUE = "consumer-receive-no-wait-queue";
const string CONSUMER_TEXT_MSG_QUEUE = "consumer-text-msg-queue";
const string CONSUMER_BYTES_MSG_QUEUE = "consumer-bytes-msg-queue";
const string CONSUMER_MAP_MSG_QUEUE = "consumer-map-msg-queue";
const string CONSUMER_PROPERTIES_QUEUE = "consumer-properties-queue";
const string CONSUMER_CORRELATION_ID_QUEUE = "consumer-correlation-id-queue";
const string CONSUMER_TIMEOUT_QUEUE = "consumer-timeout-queue";
const string CONSUMER_SELECTOR_QUEUE = "consumer-selector-queue";

// Client acknowledge test queues
const string CLIENT_ACK_QUEUE = "client-ack-queue";
const string CLIENT_ACK_MULTIPLE_QUEUE = "client-ack-multiple-queue";
const string CLIENT_ACK_NO_ACK_QUEUE = "client-ack-no-ack-queue";
const string CLIENT_ACK_MSG_TYPES_QUEUE = "client-ack-msg-types-queue";
const string CLIENT_ACK_PROPERTIES_QUEUE = "client-ack-properties-queue";

// Transacted test queues
const string TRANSACTED_COMMIT_QUEUE = "transacted-commit-queue";
const string TRANSACTED_ROLLBACK_QUEUE = "transacted-rollback-queue";
const string TRANSACTED_MULTIPLE_COMMIT_QUEUE = "transacted-multiple-commit-queue";
const string TRANSACTED_MULTIPLE_ROLLBACK_QUEUE = "transacted-multiple-rollback-queue";
const string TRANSACTED_MIXED_QUEUE = "transacted-mixed-queue";
const string TRANSACTED_MSG_TYPES_QUEUE = "transacted-msg-types-queue";
const string TRANSACTED_PRODUCER_CONSUMER_QUEUE = "transacted-producer-consumer-queue";

// Test topics
const string CONSUMER_RECEIVE_TOPIC = "test/consumer/receive";
const string CLIENT_ACK_TOPIC = "test/client-ack/topic";
const string TRANSACTED_TOPIC = "test/transacted/topic";

// SSL/TLS test queues
const string SSL_TEST_QUEUE = "ssl-test-queue";
const string SSL_PRODUCER_TEXT_QUEUE = "ssl-producer-text-queue";
const string SSL_PRODUCER_BYTES_QUEUE = "ssl-producer-bytes-queue";
const string SSL_PRODUCER_MAP_QUEUE = "ssl-producer-map-queue";
const string SSL_CONSUMER_QUEUE = "ssl-consumer-queue";
const string SSL_TRANSACTED_QUEUE = "ssl-transacted-queue";
const string SSL_CLIENT_CERT_QUEUE = "ssl-client-cert-queue";

// SSL/TLS test topics
const string SSL_TEST_TOPIC = "test/ssl/topic";

// Test message content
const string TEXT_MESSAGE_CONTENT = "Hello from Ballerina Solace Connector";
const string TEXT_MESSAGE_CONTENT_2 = "Second test message";
