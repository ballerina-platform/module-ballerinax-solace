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
// Consumer Initialization Tests
// ========================================

@test:Config {groups: ["consumer", "init"]}
isolated function testConsumerInitWithQueue() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
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
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            topicName: CONSUMER_DIRECT_TOPIC,
            endpointType: DEFAULT
        }
    });

    check consumer->close();
}

@test:Config {groups: ["consumer", "init"]}
isolated function testConsumerInitWithDurableTopic() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            topicName: CONSUMER_DURABLE_TOPIC,
            endpointType: DURABLE,
            endpointName: CONSUMER_DURABLE_ENDPOINT
        }
    });

    check consumer->close();
}

@test:Config {groups: ["consumer", "init"]}
isolated function testConsumerInitWithSelector() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_SELECTOR_QUEUE,
            selector: "priority > 5"
        }
    });

    check consumer->close();
}

@test:Config {groups: ["consumer", "init", "negative"]}
isolated function testConsumerInitInvalidQueue() returns error? {
    MessageConsumer|error consumer = new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
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
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_FLOW_QUEUE,
            transportWindowSize: 200,
            ackThreshold: 50,
            ackTimerInMsecs: 500,
            startState: true
        }
    });

    check consumer->close();
}

@test:Config {groups: ["consumer", "init"]}
isolated function testConsumerInitTransacted() returns error? {
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

// ========================================
// Consumer Receive Tests
// ========================================

// Helper function to send a message for consumer tests
isolated function sendMessageToQueue(string queueName, string content) returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: queueName},
        {payload: content.toBytes()}
    );

    check producer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveTextFromQueue() returns error? {
    // Send message first
    check sendMessageToQueue(CONSUMER_TEXT_QUEUE, TEXT_MESSAGE_CONTENT);

    // Receive message
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_TEXT_QUEUE}
    });

    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is Message, "Should receive a message");
    if msg is Message {
        test:assertEquals(msg.payload, TEXT_MESSAGE_CONTENT.toBytes(), "Payload should match");
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveTimeout() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
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

// TODO: recieveNoWait fails intermittently
@test:Config {groups: ["consumer", "receive"], enable: false}
isolated function testConsumerReceiveNoWait() returns error? {
    // Send message first
    check sendMessageToQueue(CONSUMER_NOWAIT_QUEUE, "NoWait Message");

    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_NOWAIT_QUEUE}
    });

    // Non-blocking receive
    Message? msg = check consumer->receiveNoWait();

    test:assertTrue(msg is Message, "Should receive a message with receiveNoWait");
    if msg is Message {
        test:assertEquals(msg.payload, "NoWait Message".toBytes(), "Payload should match");
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveBinaryFromQueue() returns error? {
    // Send binary message
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    byte[] BINARY_MESSAGE_CONTENT = "Hello".toBytes();

    check producer->send(
        {queueName: CONSUMER_BINARY_QUEUE},
        {payload: BINARY_MESSAGE_CONTENT}
    );

    check producer->close();

    // Receive binary message
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_BINARY_QUEUE}
    });

    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is Message, "Should receive a message");
    if msg is Message {
        test:assertEquals(msg.payload, BINARY_MESSAGE_CONTENT, "Binary payload should match");
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveWithProperties() returns error? {
    // Send message with properties
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: CONSUMER_PROPERTIES_QUEUE},
        {
        payload: TEXT_MESSAGE_CONTENT.toBytes(),
        properties: {
            "orderType": "URGENT",
            "priority": 5,
            "customerId": "12345"
        }
    }
    );

    check producer->close();

    // Receive and verify properties
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_PROPERTIES_QUEUE}
    });

    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is Message, "Should receive a message");
    if msg is Message {
        test:assertTrue(msg.properties is map<anydata>, "Should have properties");
        if msg.properties is map<anydata> {
            test:assertEquals(msg.properties["orderType"], "URGENT", "orderType should match");
            test:assertEquals(msg.properties["priority"], 5, "priority should match");
            test:assertEquals(msg.properties["customerId"], "12345", "customerId should match");
        }
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveWithMetadata() returns error? {
    // Send message with metadata
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: CONSUMER_METADATA_QUEUE},
        {
        payload: TEXT_MESSAGE_CONTENT.toBytes(),
        correlationId: "test-corr-id",
        applicationMessageId: "test-app-msg-id",
        applicationMessageType: "TEST_TYPE",
        senderId: "test-sender"
    }
    );

    check producer->close();

    // Receive and verify metadata
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
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
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            topicName: CONSUMER_DIRECT_TOPIC,
            endpointType: DEFAULT
        }
    });

    // Send message to topic
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {topicName: CONSUMER_DIRECT_TOPIC},
        {payload: "Direct topic message".toBytes()}
    );

    check producer->close();

    // Receive from topic
    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is Message, "Should receive message from direct topic");
    if msg is Message {
        test:assertEquals(msg.payload, "Direct topic message".toBytes(), "Payload should match");
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "receive"]}
isolated function testConsumerReceiveWithSelector() returns error? {
    // Send message that matches selector
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    // Message that doesn't match selector (should be filtered out)
    check producer->send(
        {queueName: CONSUMER_SELECTOR_QUEUE},
        {
        payload: "Normal message".toBytes(),
        properties: {
            "messageType": "NORMAL",
            "level": 2
        }
    }
    );

    // Message that matches selector (messageType = 'URGENT')
    check producer->send(
        {queueName: CONSUMER_SELECTOR_QUEUE},
        {
        payload: "Urgent message".toBytes(),
        properties: {
            "messageType": "URGENT",
            "level": 10
        }
    }
    );

    check producer->close();

    // Receive with selector (filter by custom property)
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_SELECTOR_QUEUE,
            selector: "messageType = 'URGENT'"
        }
    });

    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is Message, "Should receive urgent message");
    if msg is Message {
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
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: CONSUMER_MULTIPLE_QUEUE},
        {payload: "Message 1".toBytes()}
    );

    check producer->send(
        {queueName: CONSUMER_MULTIPLE_QUEUE},
        {payload: "Message 2".toBytes()}
    );

    check producer->send(
        {queueName: CONSUMER_MULTIPLE_QUEUE},
        {payload: "Message 3".toBytes()}
    );

    check producer->close();

    // Receive multiple messages
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_MULTIPLE_QUEUE}
    });

    Message? msg1 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg1 is Message, "Should receive first message");
    if msg1 is Message {
        test:assertEquals(msg1.payload, "Message 1".toBytes(), "First message payload should match");
    }

    Message? msg2 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg2 is Message, "Should receive second message");
    if msg2 is Message {
        test:assertEquals(msg2.payload, "Message 2".toBytes(), "Second message payload should match");
    }

    Message? msg3 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg3 is Message, "Should receive third message");
    if msg3 is Message {
        test:assertEquals(msg3.payload, "Message 3".toBytes(), "Third message payload should match");
    }

    check consumer->close();
}
