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

import ballerina/lang.runtime;
import ballerina/test;

@test:Config {groups: ["consumer", "client_ack"]}
isolated function testClientAckWithQueue() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: CLIENT_ACK_QUEUE}
    });

    Message message = {
        payload: "Client ack test message"
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
            queueName: CLIENT_ACK_QUEUE,
            sessionAckMode: CLIENT_ACKNOWLEDGE
        }
    });

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive a message");

    if receivedMessage is Message {
        test:assertEquals(receivedMessage.payload, "Client ack test message");
        check consumer->acknowledge(receivedMessage);
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "client_ack"], dependsOn: [testClientAckWithQueue]}
isolated function testClientAckMultipleMessages() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: CLIENT_ACK_MULTIPLE_QUEUE}
    });

    check producer->send({payload: "Message 1"});
    check producer->send({payload: "Message 2"});
    check producer->send({payload: "Message 3"});
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CLIENT_ACK_MULTIPLE_QUEUE,
            sessionAckMode: CLIENT_ACKNOWLEDGE
        }
    });

    // Receive first message
    Message? msg1 = check consumer->receive(5.0);
    test:assertTrue(msg1 is Message, "Should receive first message");
    if msg1 is Message {
        test:assertEquals(msg1.payload, "Message 1");
    }

    // Receive second message
    Message? msg2 = check consumer->receive(5.0);
    test:assertTrue(msg2 is Message, "Should receive second message");
    if msg2 is Message {
        test:assertEquals(msg2.payload, "Message 2");
    }

    // Receive third message
    Message? msg3 = check consumer->receive(5.0);
    test:assertTrue(msg3 is Message, "Should receive third message");
    if msg3 is Message {
        test:assertEquals(msg3.payload, "Message 3");
        // Acknowledge all messages (acknowledging the last one acknowledges all)
        check consumer->acknowledge(msg3);
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "client_ack"], dependsOn: [testClientAckMultipleMessages]}
isolated function testClientAckWithoutAcknowledge() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: CLIENT_ACK_NO_ACK_QUEUE}
    });

    Message message = {
        payload: "Unacknowledged message"
    };
    check producer->send(message);
    check producer->close();

    MessageConsumer consumer1 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CLIENT_ACK_NO_ACK_QUEUE,
            sessionAckMode: CLIENT_ACKNOWLEDGE
        }
    });

    Message? receivedMessage1 = check consumer1->receive(5.0);
    test:assertTrue(receivedMessage1 is Message, "Should receive message");
    if receivedMessage1 is Message {
        test:assertEquals(receivedMessage1.payload, "Unacknowledged message");
    }
    check consumer1->close();

    runtime:sleep(1.0);

    MessageConsumer consumer2 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CLIENT_ACK_NO_ACK_QUEUE,
            sessionAckMode: CLIENT_ACKNOWLEDGE
        }
    });

    Message? receivedMessage2 = check consumer2->receive(5.0);
    test:assertTrue(receivedMessage2 is Message, "Should receive redelivered message");
    if receivedMessage2 is Message {
        test:assertEquals(receivedMessage2.payload, "Unacknowledged message");
        check consumer2->acknowledge(receivedMessage2);
    }
    check consumer2->close();
}

@test:Config {groups: ["consumer", "client_ack"], dependsOn: [testClientAckWithoutAcknowledge]}
isolated function testClientAckWithTopic() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            topicName: CLIENT_ACK_TOPIC,
            sessionAckMode: CLIENT_ACKNOWLEDGE
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
        destination: {topicName: CLIENT_ACK_TOPIC}
    });

    Message message = {
        payload: "Topic client ack message"
    };
    check producer->send(message);
    check producer->close();

    // Receive and acknowledge
    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive message from topic");
    if receivedMessage is Message {
        test:assertEquals(receivedMessage.payload, "Topic client ack message");
        check consumer->acknowledge(receivedMessage);
    }
    check consumer->close();
}

@test:Config {groups: ["consumer", "client_ack"], dependsOn: [testClientAckWithTopic]}
isolated function testClientAckWithDifferentMessageTypes() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: CLIENT_ACK_MSG_TYPES_QUEUE}
    });

    check producer->send({payload: "Text message"});
    check producer->send({payload: [1, 2, 3, 4, 5]});
    check producer->send({payload: {"key": "value", "number": 42}});
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CLIENT_ACK_MSG_TYPES_QUEUE,
            sessionAckMode: CLIENT_ACKNOWLEDGE
        }
    });

    Message? msg1 = check consumer->receive(5.0);
    test:assertTrue(msg1 is Message);
    if msg1 is Message {
        test:assertTrue(msg1.payload is string);
        check consumer->acknowledge(msg1);
    }

    Message? msg2 = check consumer->receive(5.0);
    test:assertTrue(msg2 is Message);
    if msg2 is Message {
        test:assertTrue(msg2.payload is byte[]);
        check consumer->acknowledge(msg2);
    }

    Message? msg3 = check consumer->receive(5.0);
    test:assertTrue(msg3 is Message);
    if msg3 is Message {
        test:assertTrue(msg3.payload is map<Value>);
        check consumer->acknowledge(msg3);
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "client_ack"], dependsOn: [testClientAckWithDifferentMessageTypes]}
isolated function testClientAckWithMessageProperties() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: CLIENT_ACK_PROPERTIES_QUEUE}
    });

    Message message = {
        payload: "Message with properties",
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
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: CLIENT_ACK_PROPERTIES_QUEUE,
            sessionAckMode: CLIENT_ACKNOWLEDGE
        }
    });

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message);
    if receivedMessage is Message {
        test:assertEquals(receivedMessage.payload, "Message with properties");
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
