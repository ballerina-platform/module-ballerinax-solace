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

import ballerina/test;

// ========================================
// Consumer Transaction Tests
// ========================================

@test:Config {groups: ["consumer", "transacted"]}
isolated function testConsumerTransactedInit() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TX_COMMIT_QUEUE}
    });

    check consumer->close();
}

@test:Config {groups: ["consumer", "transacted"], dependsOn: [testConsumerTransactedInit]}
isolated function testConsumerTransactedCommit() returns error? {
    // Send message
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: CONSUMER_TX_COMMIT_QUEUE},
        {payload: "Transacted commit message".toBytes()}
    );

    check producer->close();

    // Receive and commit
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TX_COMMIT_QUEUE}
    });

    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is Message, "Should receive a message");

    // Commit transaction (acknowledges the message)
    check consumer->'commit();

    check consumer->close();

    // Verify message is not redelivered
    MessageConsumer consumer2 = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TX_COMMIT_QUEUE}
    });

    Message? noMsg = check consumer2->receive(SHORT_RECEIVE_TIMEOUT);

    test:assertTrue(noMsg is (), "Message should not be redelivered after commit");

    check consumer2->close();
}

@test:Config {groups: ["consumer", "transacted"], dependsOn: [testConsumerTransactedInit]}
isolated function testConsumerTransactedRollback() returns error? {
    // Send message
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: CONSUMER_TX_ROLLBACK_QUEUE},
        {payload: "Transacted rollback message".toBytes()}
    );

    check producer->close();

    // Receive and rollback
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TX_ROLLBACK_QUEUE}
    });

    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is Message, "Should receive a message");

    // Rollback transaction (message requeued)
    check consumer->'rollback();

    check consumer->close();

    // Verify message is redelivered
    MessageConsumer consumer2 = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TX_ROLLBACK_QUEUE}
    });

    Message? redeliveredMsg = check consumer2->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(redeliveredMsg is Message, "Message should be redelivered after rollback");

    if redeliveredMsg is Message {
        test:assertEquals(redeliveredMsg.payload, "Transacted rollback message".toBytes(), "Redelivered message should match");
        test:assertTrue(redeliveredMsg.redelivered == true, "Redelivered flag should be set");
    }

    check consumer2->close();
}

@test:Config {groups: ["consumer", "transacted"], dependsOn: [testConsumerTransactedInit]}
isolated function testConsumerTransactedMultipleMessages() returns error? {
    // Send multiple messages
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: CONSUMER_TX_MULTIPLE_QUEUE},
        {payload: "TX Message 1".toBytes()}
    );

    check producer->send(
        {queueName: CONSUMER_TX_MULTIPLE_QUEUE},
        {payload: "TX Message 2".toBytes()}
    );

    check producer->send(
        {queueName: CONSUMER_TX_MULTIPLE_QUEUE},
        {payload: "TX Message 3".toBytes()}
    );

    check producer->close();

    // Receive all and commit
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TX_MULTIPLE_QUEUE}
    });

    Message? msg1 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg1 is Message, "Should receive first message");

    Message? msg2 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg2 is Message, "Should receive second message");

    Message? msg3 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg3 is Message, "Should receive third message");

    // Commit all
    check consumer->'commit();

    check consumer->close();

    // Verify no messages are redelivered
    MessageConsumer consumer2 = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TX_MULTIPLE_QUEUE}
    });

    Message? noMsg = check consumer2->receive(SHORT_RECEIVE_TIMEOUT);

    test:assertTrue(noMsg is (), "No messages should be redelivered after commit");

    check consumer2->close();
}

@test:Config {groups: ["consumer", "transacted"], dependsOn: [testConsumerTransactedInit]}
isolated function testConsumerTransactedMixedOperations() returns error? {
    // Send messages
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: CONSUMER_TX_MIXED_QUEUE},
        {payload: "Mixed TX Message 1".toBytes()}
    );

    check producer->send(
        {queueName: CONSUMER_TX_MIXED_QUEUE},
        {payload: "Mixed TX Message 2".toBytes()}
    );

    check producer->close();

    // Receive, commit, then receive, rollback
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TX_MIXED_QUEUE}
    });

    // First transaction - receive and commit
    Message? msg1 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg1 is Message, "Should receive first message");
    check consumer->'commit();

    // Second transaction - receive and rollback
    Message? msg2 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg2 is Message, "Should receive second message");
    check consumer->'rollback();

    check consumer->close();

    // Verify only second message is redelivered
    MessageConsumer consumer2 = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TX_MIXED_QUEUE}
    });

    Message? redeliveredMsg = check consumer2->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(redeliveredMsg is Message, "Second message should be redelivered after rollback");

    if redeliveredMsg is Message {
        test:assertEquals(redeliveredMsg.payload, "Mixed TX Message 2".toBytes(), "Should be the second message");
    }

    check consumer2->close();
}

@test:Config {groups: ["consumer", "transacted", "negative"]}
isolated function testConsumerCommitWithoutTransaction() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: false, // Non-transacted mode
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TX_COMMIT_QUEUE}
    });

    // Attempt to commit without transaction
    error? result = consumer->'commit();

    test:assertTrue(result is error, "Commit without transaction should return error");

    check consumer->close();
}

@test:Config {groups: ["consumer", "transacted", "negative"]}
isolated function testConsumerTransactedDirectTopicError() returns error? {
    // Attempt to create transacted consumer with direct topic (should fail)
    MessageConsumer|error consumer = new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            topicName: CONSUMER_DIRECT_TOPIC,
            endpointType: DEFAULT
        }
    });

    test:assertTrue(consumer is error, "Transacted consumer with direct topic should return error");
}

@test:Config {groups: ["consumer", "transacted", "producer"]}
isolated function testProducerConsumerCoordinatedTransaction() returns error? {
    // Transacted producer sends messages
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: CONSUMER_TX_COORDINATED_QUEUE},
        {
        payload: "Coordinated TX Message 1".toBytes(),
        deliveryMode: PERSISTENT
    }
    );

    check producer->send(
        {queueName: CONSUMER_TX_COORDINATED_QUEUE},
        {
        payload: "Coordinated TX Message 2".toBytes(),
        deliveryMode: PERSISTENT
    }
    );

    // Commit producer transaction
    check producer->'commit();

    check producer->close();

    // Transacted consumer receives messages
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TX_COORDINATED_QUEUE}
    });

    Message? msg1 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg1 is Message, "Should receive first message after producer commit");

    Message? msg2 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg2 is Message, "Should receive second message after producer commit");

    // Commit consumer transaction
    check consumer->'commit();

    check consumer->close();

    // Verify no messages remain
    MessageConsumer consumer2 = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TX_COORDINATED_QUEUE}
    });

    Message? noMsg = check consumer2->receive(SHORT_RECEIVE_TIMEOUT);

    test:assertTrue(noMsg is (), "No messages should remain after consumer commit");

    check consumer2->close();
}

@test:Config {groups: ["transacted", "endtoend"]}
isolated function testEndToEndTransaction() returns error? {
    string E2E_QUEUE = "test/producer/tx/commit/queue";

    // Transacted producer
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    // Send messages in transaction
    check producer->send(
        {queueName: E2E_QUEUE},
        {payload: "E2E Message 1".toBytes(), deliveryMode: PERSISTENT}
    );

    check producer->send(
        {queueName: E2E_QUEUE},
        {payload: "E2E Message 2".toBytes(), deliveryMode: PERSISTENT}
    );

    // Commit producer
    check producer->'commit();
    check producer->close();

    // Transacted consumer
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: E2E_QUEUE}
    });

    // Receive messages in transaction
    Message? msg1 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg1 is Message, "Should receive first message");

    Message? msg2 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg2 is Message, "Should receive second message");

    // Commit consumer
    check consumer->'commit();
    check consumer->close();
}
