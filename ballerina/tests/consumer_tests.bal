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

@test:Config {groups: ["consumer"]}
isolated function testConsumerInitWithQueue() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_INIT_QUEUE
        }
    });
    check consumer->close();
}

@test:Config {groups: ["consumer"]}
isolated function testConsumerInitWithTopic() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            topicName: TEST_TOPIC
        }
    });
    check consumer->close();
}

@test:Config {groups: ["consumer"]}
isolated function testReceiveWithQueue() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: CONSUMER_RECEIVE_QUEUE}
    });

    Message message = {
        content: TEXT_MESSAGE_CONTENT
    };
    check producer->send(message);
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_RECEIVE_QUEUE
        }
    });

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive a message");
    if receivedMessage is Message {
        test:assertEquals(receivedMessage.content, TEXT_MESSAGE_CONTENT,
            "Message content should match");
    }
    check consumer->close();
}

@test:Config {groups: ["consumer", "consumerFix"], dependsOn: [testReceiveWithQueue], enable: false}
isolated function testReceiveNoWaitWithQueue() returns error? {
    MessageConsumer consumer1 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_RECEIVE_NO_WAIT_QUEUE
        }
    });

    Message? emptyMessage = check consumer1->receiveNoWait();
    test:assertTrue(emptyMessage is (), "Should not receive message when queue is empty");
    check consumer1->close();

    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: CONSUMER_RECEIVE_NO_WAIT_QUEUE}
    });

    Message message = {
        content: TEXT_MESSAGE_CONTENT_2
    };

    MessageConsumer consumer2 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_RECEIVE_NO_WAIT_QUEUE
        }
    });

    check producer->send(message);
    check producer->close();

    Message? receivedMessage = check consumer2->receiveNoWait();
    test:assertTrue(receivedMessage is Message, "Should receive a message");
    if receivedMessage is Message {
        test:assertEquals(receivedMessage.content, TEXT_MESSAGE_CONTENT_2,
            "Message content should match");
    }
    check consumer2->close();
}

@test:Config {groups: ["consumer"], dependsOn: [testReceiveWithQueue]}
isolated function testReceiveWithTopic() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            topicName: CONSUMER_RECEIVE_TOPIC
        }
    });

    runtime:sleep(0.5);

    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {topicName: CONSUMER_RECEIVE_TOPIC}
    });

    Message message = {
        content: "Topic message content"
    };
    check producer->send(message);
    check producer->close();

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive a message from topic");
    if receivedMessage is Message {
        test:assertEquals(receivedMessage.content, "Topic message content",
            "Message content should match");
    }
    check consumer->close();
}

@test:Config {groups: ["consumer"], dependsOn: [testReceiveWithTopic]}
isolated function testReceiveTextMessage() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: CONSUMER_TEXT_MSG_QUEUE}
    });

    Message message = {
        content: "Text message test"
    };
    check producer->send(message);
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_TEXT_MSG_QUEUE
        }
    });

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive a text message");
    if receivedMessage is Message {
        test:assertTrue(receivedMessage.content is string, "Content should be string");
        test:assertEquals(receivedMessage.content, "Text message test");
    }
    check consumer->close();
}

@test:Config {groups: ["consumer"], dependsOn: [testReceiveTextMessage]}
isolated function testReceiveBytesMessage() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: CONSUMER_BYTES_MSG_QUEUE}
    });

    byte[] byteContent = [72, 101, 108, 108, 111]; // "Hello" in bytes
    Message message = {
        content: byteContent
    };
    check producer->send(message);
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_BYTES_MSG_QUEUE
        }
    });

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive a bytes message");
    if receivedMessage is Message {
        test:assertTrue(receivedMessage.content is byte[], "Content should be byte array");
        test:assertEquals(receivedMessage.content, byteContent);
    }
    check consumer->close();
}

@test:Config {groups: ["consumer"], dependsOn: [testReceiveBytesMessage]}
isolated function testReceiveMapMessage() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: CONSUMER_MAP_MSG_QUEUE}
    });

    map<Value> mapContent = {
        "name": "John",
        "age": 30,
        "active": true
    };
    Message message = {
        content: mapContent
    };
    check producer->send(message);
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_MAP_MSG_QUEUE
        }
    });

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive a map message");
    if receivedMessage is Message {
        var content = receivedMessage.content;
        test:assertTrue(content is map<Value>, "Content should be map");
        if content is map<Value> {
            test:assertEquals(content["name"], "John");
            test:assertEquals(content["age"], 30);
            test:assertEquals(content["active"], true);
        }
    }
    check consumer->close();
}

@test:Config {groups: ["consumer"], dependsOn: [testReceiveMapMessage]}
isolated function testReceiveMessageWithProperties() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: CONSUMER_PROPERTIES_QUEUE}
    });

    Message message = {
        content: "Message with properties",
        properties: {
            "priority": "high",
            "timestamp": 123456789,
            "enabled": true
        }
    };
    check producer->send(message);
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_PROPERTIES_QUEUE
        }
    });

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive a message with properties");
    if receivedMessage is Message {
        test:assertEquals(receivedMessage.content, "Message with properties");
        test:assertTrue(receivedMessage.properties is map<Property>, "Should have properties");
        if receivedMessage.properties is map<Property> {
            test:assertEquals(receivedMessage.properties["priority"], "high");
            test:assertEquals(receivedMessage.properties["timestamp"], 123456789);
            test:assertEquals(receivedMessage.properties["enabled"], true);
        }
    }
    check consumer->close();
}

@test:Config {groups: ["consumer"], dependsOn: [testReceiveMessageWithProperties]}
isolated function testReceiveMessageWithCorrelationId() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: CONSUMER_CORRELATION_ID_QUEUE}
    });

    Message message = {
        content: "Correlated message",
        correlationId: "CORR-123-456"
    };
    check producer->send(message);
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_CORRELATION_ID_QUEUE
        }
    });

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive message with correlation ID");
    if receivedMessage is Message {
        test:assertEquals(receivedMessage.correlationId, "CORR-123-456");
    }
    check consumer->close();
}

@test:Config {groups: ["consumer"], dependsOn: [testReceiveMessageWithCorrelationId]}
isolated function testReceiveTimeout() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_TIMEOUT_QUEUE
        }
    });

    Message? receivedMessage = check consumer->receive(1.0);
    test:assertTrue(receivedMessage is (), "Should return null when no message available");
    check consumer->close();
}

@test:Config {groups: ["consumer"], dependsOn: [testReceiveTimeout]}
isolated function testMessageSelectorWithQueue() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: CONSUMER_SELECTOR_QUEUE}
    });

    Message message1 = {
        content: "Low priority message",
        properties: {"priority": "low"}
    };
    check producer->send(message1);

    Message message2 = {
        content: "High priority message",
        properties: {"priority": "high"}
    };
    check producer->send(message2);
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CONSUMER_SELECTOR_QUEUE,
            messageSelector: "priority = 'high'"
        }
    });

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive high priority message");
    if receivedMessage is Message {
        test:assertEquals(receivedMessage.content, "High priority message");
    }
    check consumer->close();
}
