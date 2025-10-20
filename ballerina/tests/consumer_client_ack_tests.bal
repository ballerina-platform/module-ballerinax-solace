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

import ballerina/test;
import ballerina/lang.runtime;

// Test CLIENT_ACKNOWLEDGE with queue
@test:Config {groups: ["consumer", "client_ack"]}
isolated function testClientAckWithQueue() returns error? {
    // Send a message
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: TEST_QUEUE}
    });

    Message message = {
        content: "Client ack test message"
    };
    check producer->send(message);
    check producer->close();

    // Consume with CLIENT_ACKNOWLEDGE
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TEST_QUEUE,
            sessionAckMode: CLIENT_ACKNOWLEDGE
        }
    });

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive a message");

    if receivedMessage is Message {
        test:assertEquals(receivedMessage.content, "Client ack test message");
        // Explicitly acknowledge the message
        check consumer->acknowledge(receivedMessage);
    }

    check consumer->close();
}

// Test multiple messages with CLIENT_ACKNOWLEDGE
@test:Config {groups: ["consumer", "client_ack"]}
isolated function testClientAckMultipleMessages() returns error? {
    // Send multiple messages
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: TEST_QUEUE}
    });

    check producer->send({content: "Message 1"});
    check producer->send({content: "Message 2"});
    check producer->send({content: "Message 3"});
    check producer->close();

    // Consume with CLIENT_ACKNOWLEDGE
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TEST_QUEUE,
            sessionAckMode: CLIENT_ACKNOWLEDGE
        }
    });

    // Receive first message
    Message? msg1 = check consumer->receive(5.0);
    test:assertTrue(msg1 is Message, "Should receive first message");
    if msg1 is Message {
        test:assertEquals(msg1.content, "Message 1");
    }

    // Receive second message
    Message? msg2 = check consumer->receive(5.0);
    test:assertTrue(msg2 is Message, "Should receive second message");
    if msg2 is Message {
        test:assertEquals(msg2.content, "Message 2");
    }

    // Receive third message
    Message? msg3 = check consumer->receive(5.0);
    test:assertTrue(msg3 is Message, "Should receive third message");
    if msg3 is Message {
        test:assertEquals(msg3.content, "Message 3");
        // Acknowledge all messages (acknowledging the last one acknowledges all)
        check consumer->acknowledge(msg3);
    }

    check consumer->close();
}

// Test CLIENT_ACKNOWLEDGE without acknowledging (message should be redelivered)
@test:Config {groups: ["consumer", "client_ack"]}
isolated function testClientAckWithoutAcknowledge() returns error? {
    // Send a message
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: TEST_QUEUE}
    });

    Message message = {
        content: "Unacknowledged message"
    };
    check producer->send(message);
    check producer->close();

    // First consumer - receive but don't acknowledge
    MessageConsumer consumer1 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TEST_QUEUE,
            sessionAckMode: CLIENT_ACKNOWLEDGE
        }
    });

    Message? receivedMessage1 = check consumer1->receive(5.0);
    test:assertTrue(receivedMessage1 is Message, "Should receive message");
    if receivedMessage1 is Message {
        test:assertEquals(receivedMessage1.content, "Unacknowledged message");
        // Close without acknowledging
    }
    check consumer1->close();

    // Small delay to allow message to return to queue
    runtime:sleep(1.0);

    // Second consumer - should receive the same message
    MessageConsumer consumer2 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TEST_QUEUE,
            sessionAckMode: CLIENT_ACKNOWLEDGE
        }
    });

    Message? receivedMessage2 = check consumer2->receive(5.0);
    test:assertTrue(receivedMessage2 is Message, "Should receive redelivered message");
    if receivedMessage2 is Message {
        test:assertEquals(receivedMessage2.content, "Unacknowledged message");
        // Acknowledge this time
        check consumer2->acknowledge(receivedMessage2);
    }
    check consumer2->close();
}

// Test CLIENT_ACKNOWLEDGE with topic
@test:Config {groups: ["consumer", "client_ack"]}
isolated function testClientAckWithTopic() returns error? {
    // Create consumer first to ensure subscription is active
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            topicName: TEST_TOPIC,
            sessionAckMode: CLIENT_ACKNOWLEDGE
        }
    });

    // Small delay to ensure subscription is established
    runtime:sleep(0.5);

    // Send a message to topic
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {topicName: TEST_TOPIC}
    });

    Message message = {
        content: "Topic client ack message"
    };
    check producer->send(message);
    check producer->close();

    // Receive and acknowledge
    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive message from topic");
    if receivedMessage is Message {
        test:assertEquals(receivedMessage.content, "Topic client ack message");
        check consumer->acknowledge(receivedMessage);
    }
    check consumer->close();
}

@test:Config {groups: ["consumer", "client_ack"]}
isolated function testClientAckWithDifferentMessageTypes() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: TEST_QUEUE}
    });

    // Send different message types
    check producer->send({content: "Text message"});
    check producer->send({content: [1, 2, 3, 4, 5]});
    check producer->send({content: {"key": "value", "number": 42}});
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TEST_QUEUE,
            sessionAckMode: CLIENT_ACKNOWLEDGE
        }
    });

    // Receive and acknowledge text message
    Message? msg1 = check consumer->receive(5.0);
    test:assertTrue(msg1 is Message);
    if msg1 is Message {
        test:assertTrue(msg1.content is string);
        check consumer->acknowledge(msg1);
    }

    // Receive and acknowledge bytes message
    Message? msg2 = check consumer->receive(5.0);
    test:assertTrue(msg2 is Message);
    if msg2 is Message {
        test:assertTrue(msg2.content is byte[]);
        check consumer->acknowledge(msg2);
    }

    // Receive and acknowledge map message
    Message? msg3 = check consumer->receive(5.0);
    test:assertTrue(msg3 is Message);
    if msg3 is Message {
        test:assertTrue(msg3.content is map<Value>);
        check consumer->acknowledge(msg3);
    }

    check consumer->close();
}

// Test CLIENT_ACKNOWLEDGE with message properties
@test:Config {groups: ["consumer", "client_ack"]}
isolated function testClientAckWithMessageProperties() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: TEST_QUEUE}
    });

    Message message = {
        content: "Message with properties",
        properties: {
            "priority": "high",
            "region": "us-west",
            "retryCount": 3
        }
    };
    check producer->send(message);
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TEST_QUEUE,
            sessionAckMode: CLIENT_ACKNOWLEDGE
        }
    });

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message);
    if receivedMessage is Message {
        test:assertEquals(receivedMessage.content, "Message with properties");
        test:assertTrue(receivedMessage.properties is map<Property>);
        if receivedMessage.properties is map<Property> {
            test:assertEquals(receivedMessage.properties["priority"], "high");
            test:assertEquals(receivedMessage.properties["region"], "us-west");
            test:assertEquals(receivedMessage.properties["retryCount"], 3);
        }
        check consumer->acknowledge(receivedMessage);
    }
    check consumer->close();
}
