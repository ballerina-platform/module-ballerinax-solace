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

import ballerina/lang.runtime;
import ballerina/test;

// ========================================
// Consumer Initialization Tests
// ========================================

@test:Config {groups: ["consumer", "init"]}
isolated function testConsumerInitWithQueue() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_INIT_QUEUE}
    });

    check consumer->close();
}

@test:Config {groups: ["consumer", "init"]}
isolated function testConsumerInitWithDirectTopic() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            topicName: CONSUMER_DIRECT_TOPIC,
            durability: TEMPORARY
        }
    });

    check consumer->close();
}

@test:Config {groups: ["consumer", "init"]}
isolated function testConsumerInitWithDurableTopic() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            topicName: CONSUMER_DURABLE_TOPIC,
            durability: DURABLE,
            endpointName: CONSUMER_DURABLE_ENDPOINT
        }
    });

    check consumer->close();
}

@test:Config {groups: ["consumer", "init"]}
isolated function testConsumerInitWithSelector() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_SELECTOR_QUEUE,
            messageSelector: "priority > 5"
        }
    });

    check consumer->close();
}

@test:Config {groups: ["consumer", "init", "negative"]}
isolated function testConsumerInitInvalidQueue() returns error? {
    MessageConsumer|error consumer = new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: "nonexistent/queue"}
    });

    test:assertTrue(consumer is error, "Initialization with invalid queue should fail");

}

@test:Config {groups: ["consumer", "init"]}
isolated function testConsumerInitWithFlowProperties() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_FLOW_QUEUE,
            transportWindowSize: 200,
            ackThreshold: 50,
            ackTimer: 0.5
        }
    });

    check consumer->close();
}

@test:Config {groups: ["consumer", "init", "validation", "negative"]}
isolated function testConsumerInitWithTransportWindowSizeTooHigh() returns error? {
    MessageConsumer|error consumer = new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD},
        subscriptionConfig: {
            queueName: CONSUMER_FLOW_QUEUE,
            transportWindowSize: 256
        }
    });

    test:assertTrue(consumer is error, "transportWindowSize above 255 should fail validation");
    if consumer is error {
        test:assertEquals(consumer.message(), "Failed to initialize consumer: transportWindowSize must be between 1 and 255");
    }
}

@test:Config {groups: ["consumer", "init", "validation", "negative"]}
isolated function testConsumerInitWithAckThresholdTooHigh() returns error? {
    MessageConsumer|error consumer = new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD},
        subscriptionConfig: {
            queueName: CONSUMER_FLOW_QUEUE,
            ackThreshold: 76
        }
    });

    test:assertTrue(consumer is error, "ackThreshold above 75 should fail validation");
    if consumer is error {
        test:assertEquals(consumer.message(), "Failed to initialize consumer: ackThreshold must be between 1 and 75");
    }
}

@test:Config {groups: ["consumer", "init", "validation", "negative"]}
isolated function testConsumerInitWithAckTimerTooHigh() returns error? {
    MessageConsumer|error consumer = new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD},
        subscriptionConfig: {
            queueName: CONSUMER_FLOW_QUEUE,
            ackTimer: 2.0
        }
    });

    test:assertTrue(consumer is error, "ackTimer above 1.5 seconds should fail validation");
    if consumer is error {
        test:assertEquals(consumer.message(), "Failed to initialize consumer: ackTimer must be between 0.02 and 1.5 seconds");
    }
}

@test:Config {groups: ["consumer", "init", "validation", "negative"]}
isolated function testConsumerInitWithDurableQueueMissingName() returns error? {
    MessageConsumer|error consumer = new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD},
        subscriptionConfig: {durability: DURABLE}
    });

    test:assertTrue(consumer is error, "A DURABLE queue with no queueName should fail validation");
    if consumer is error {
        test:assertEquals(consumer.message(),
                "Failed to initialize consumer: queueName is required when durability is not TEMPORARY");
    }
}

@test:Config {groups: ["consumer", "init"]}
isolated function testConsumerInitWithTemporaryQueueNoName() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD},
        subscriptionConfig: {durability: TEMPORARY}
    });

    string destinationName = consumer->destinationName();
    test:assertTrue(destinationName.length() > 0, "A TEMPORARY queue should resolve to a broker-generated name");

    check consumer->close();
}

@test:Config {groups: ["consumer", "init"]}
isolated function testConsumerDestinationNameForTopic() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD},
        subscriptionConfig: {topicName: CONSUMER_DIRECT_TOPIC}
    });

    string destinationName = consumer->destinationName();
    test:assertEquals(destinationName, CONSUMER_DIRECT_TOPIC, "destinationName() should return the topic name");

    check consumer->close();
}

@test:Config {groups: ["consumer", "init"]}
isolated function testConsumerInitTransacted() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TX_COMMIT_QUEUE}
    });

    check consumer->close();
}

// ========================================
// Consumer Receive Tests
// ========================================

// Helper function to send a message for consumer tests
isolated function sendMessageToQueue(string queueName, string content) returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {payload: content.toBytes()},
        {queueName: queueName}
    );

    check producer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveTextFromQueue() returns error? {
    // Send message first
    check sendMessageToQueue(CONSUMER_TEXT_QUEUE, TEXT_MESSAGE_CONTENT);

    // Receive message
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TEXT_QUEUE}
    });

    BytesPayloadMessage? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is BytesPayloadMessage, "Should receive a message");
    if msg is BytesPayloadMessage {
        test:assertEquals(msg.payload, TEXT_MESSAGE_CONTENT.toBytes(), "Payload should match");
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveTimeout() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TIMEOUT_QUEUE}
    });

    // Receive from empty queue with short timeout
    Message? msg = check consumer->receive(SHORT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is (), "Should return null on timeout");

    check consumer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveNoWait() returns error? {
    // Send message first
    check sendMessageToQueue(CONSUMER_NOWAIT_QUEUE, "NoWait Message");

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_NOWAIT_QUEUE}
    });

    BytesPayloadMessage? msg = ();
    foreach int _attempt in 0 ..< NOWAIT_POLL_MAX_ATTEMPTS {
        msg = check consumer->receiveNoWait();
        if msg is BytesPayloadMessage {
            break;
        }
        runtime:sleep(NOWAIT_POLL_INTERVAL);
    }

    test:assertTrue(msg is BytesPayloadMessage, "Should receive a message with receiveNoWait");
    if msg is BytesPayloadMessage {
        test:assertEquals(msg.payload, "NoWait Message".toBytes(), "Payload should match");
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveBinaryFromQueue() returns error? {
    // Send binary message
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    byte[] BINARY_MESSAGE_CONTENT = "Hello".toBytes();

    check producer->send(
        {payload: BINARY_MESSAGE_CONTENT},
        {queueName: CONSUMER_BINARY_QUEUE}
    );

    check producer->close();

    // Receive binary message
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_BINARY_QUEUE}
    });

    BytesPayloadMessage? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is BytesPayloadMessage, "Should receive a message");
    if msg is BytesPayloadMessage {
        test:assertEquals(msg.payload, BINARY_MESSAGE_CONTENT, "Binary payload should match");
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveWithProperties() returns error? {
    // Send message with properties
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    byte[] binaryProp = [1, 2, 3, 4];
    check producer->send(
        {
        payload: TEXT_MESSAGE_CONTENT.toBytes(),
        properties: {
            "orderType": "URGENT",
            "priority": 5,
            "customerId": "12345",
            "binaryProp": binaryProp,
            "nestedProp": {"region": "EU", "retryCount": 2}
        }
    },
        {queueName: CONSUMER_PROPERTIES_QUEUE}
    );

    check producer->close();

    // Receive and verify properties
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_PROPERTIES_QUEUE}
    });

    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is Message, "Should receive a message");
    if msg is Message {
        test:assertTrue(msg.properties is map<Property>, "Should have properties");
        if msg.properties is map<Property> {
            test:assertEquals(msg.properties["orderType"], "URGENT", "orderType should match");
            test:assertEquals(msg.properties["priority"], 5, "priority should match");
            test:assertEquals(msg.properties["customerId"], "12345", "customerId should match");
            test:assertEquals(msg.properties["binaryProp"], binaryProp, "binaryProp should match");
            test:assertEquals(msg.properties["nestedProp"], {"region": "EU", "retryCount": 2},
                                                            "nestedProp should match");
        }
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveWithMetadata() returns error? {
    // Send message with metadata
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {
        payload: TEXT_MESSAGE_CONTENT.toBytes(),
        correlationId: "test-corr-id",
        applicationMessageId: "test-app-msg-id",
        applicationMessageType: "TEST_TYPE",
        senderId: "test-sender"
    },
        {queueName: CONSUMER_METADATA_QUEUE}
    );

    check producer->close();

    // Receive and verify metadata
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_METADATA_QUEUE}
    });

    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is Message, "Should receive a message");
    if msg is Message {
        test:assertEquals(msg.correlationId, "test-corr-id", "correlationId should match");
        test:assertEquals(msg.applicationMessageId, "test-app-msg-id", "applicationMessageId should match");
        test:assertEquals(msg.applicationMessageType, "TEST_TYPE", "applicationMessageType should match");
        test:assertEquals(msg.senderId, "test-sender", "senderId should match");
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveFromDirectTopic() returns error? {
    // Create consumer first for direct topic (must be subscribed before sending)
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            topicName: CONSUMER_DIRECT_TOPIC,
            durability: TEMPORARY
        }
    });

    // Send message to topic
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {payload: "Direct topic message".toBytes()},
        {topicName: CONSUMER_DIRECT_TOPIC}
    );

    check producer->close();

    // Receive from topic
    BytesPayloadMessage? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is BytesPayloadMessage, "Should receive message from direct topic");
    if msg is BytesPayloadMessage {
        test:assertEquals(msg.payload, "Direct topic message".toBytes(), "Payload should match");
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveWithSelector() returns error? {
    // Send message that matches selector
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    // Message that doesn't match selector (should be filtered out)
    check producer->send(
        {
        payload: "Normal message".toBytes(),
        properties: {
            "messageType": "NORMAL",
            "level": 2
        }
    },
        {queueName: CONSUMER_SELECTOR_QUEUE}
    );

    // Message that matches selector (messageType = 'URGENT')
    check producer->send(
        {
        payload: "Urgent message".toBytes(),
        properties: {
            "messageType": "URGENT",
            "level": 10
        }
    },
        {queueName: CONSUMER_SELECTOR_QUEUE}
    );

    check producer->close();

    // Receive with selector (filter by custom property)
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_SELECTOR_QUEUE,
            messageSelector: "messageType = 'URGENT'"
        }
    });

    BytesPayloadMessage? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is BytesPayloadMessage, "Should receive urgent message");
    if msg is BytesPayloadMessage {
        test:assertEquals(msg.payload, "Urgent message".toBytes(), "Should receive only urgent message");
    }

    // Ensure no more messages are received
    Message? noMsg = check consumer->receive(SHORT_RECEIVE_TIMEOUT);
    test:assertTrue(noMsg is (), "No more messages should be received");

    check consumer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveMultipleMessages() returns error? {
    // Send multiple messages
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {payload: "Message 1".toBytes()},
        {queueName: CONSUMER_MULTIPLE_QUEUE}
    );

    check producer->send(
        {payload: "Message 2".toBytes()},
        {queueName: CONSUMER_MULTIPLE_QUEUE}
    );

    check producer->send(
        {payload: "Message 3".toBytes()},
        {queueName: CONSUMER_MULTIPLE_QUEUE}
    );

    check producer->close();

    // Receive multiple messages
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_MULTIPLE_QUEUE}
    });

    BytesPayloadMessage? msg1 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg1 is BytesPayloadMessage, "Should receive first message");
    if msg1 is BytesPayloadMessage {
        test:assertEquals(msg1.payload, "Message 1".toBytes(), "First message payload should match");
    }

    BytesPayloadMessage? msg2 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg2 is BytesPayloadMessage, "Should receive second message");
    if msg2 is BytesPayloadMessage {
        test:assertEquals(msg2.payload, "Message 2".toBytes(), "Second message payload should match");
    }

    BytesPayloadMessage? msg3 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg3 is BytesPayloadMessage, "Should receive third message");
    if msg3 is BytesPayloadMessage {
        test:assertEquals(msg3.payload, "Message 3".toBytes(), "Third message payload should match");
    }

    check consumer->close();
}

// ========================================
// Default Acknowledgement Mode Tests
// ========================================

@test:Config {groups: ["consumer", "ackmode"]}
isolated function testConsumerDefaultAckModeDoesNotRedeliver() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {payload: "Message for default ack mode test".toBytes()},
        {queueName: ACK_DEFAULT_MODE_QUEUE}
    );

    check producer->close();

    // First consumer - ackMode intentionally omitted, must default to AUTO_ACK. Receive the
    // message without calling ack(), then close.
    MessageConsumer consumer1 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: ACK_DEFAULT_MODE_QUEUE}
    });

    Message? msg1 = check consumer1->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg1 is Message, "First consumer should receive message");
    if msg1 is Message {
        test:assertTrue(msg1.redelivered != true, "First delivery should not be marked as redelivered");
    }

    check consumer1->close();

    // Second consumer on the same queue - if the default AUTO_ACK mode actually behaved as
    // CLIENT_ACK, the never-acknowledged message would be redelivered here.
    MessageConsumer consumer2 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: ACK_DEFAULT_MODE_QUEUE}
    });

    Message? msg2 = check consumer2->receive(SHORT_RECEIVE_TIMEOUT);
    test:assertTrue(msg2 is (), "Message must not be redelivered under the default (AUTO_ACK) mode");

    check consumer2->close();
}
