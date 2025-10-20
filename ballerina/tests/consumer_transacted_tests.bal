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

@test:Config {groups: ["consumer", "transacted", "consumerFix"]}
isolated function testTransactedSessionWithCommit() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: TRANSACTED_COMMIT_QUEUE}
    });

    Message message = {
        payload: "Transacted commit test"
    };
    check producer->send(message);
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TRANSACTED_COMMIT_QUEUE,
            sessionAckMode: SESSION_TRANSACTED
        }
    });

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive a message");
    if receivedMessage is Message {
        test:assertEquals(receivedMessage.payload, "Transacted commit test");
    }

    check consumer->'commit();
    check consumer->close();

    MessageConsumer consumer2 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TRANSACTED_COMMIT_QUEUE,
            sessionAckMode: SESSION_TRANSACTED
        }
    });

    Message? shouldBeNull = check consumer2->receive(1.0);
    test:assertTrue(shouldBeNull is (), "Message should not be redelivered after commit");
    check consumer2->close();
}

@test:Config {groups: ["consumer", "transacted"], dependsOn: [testTransactedSessionWithCommit]}
isolated function testTransactedSessionWithRollback() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: TRANSACTED_ROLLBACK_QUEUE}
    });

    Message message = {
        payload: "Transacted rollback test"
    };
    check producer->send(message);
    check producer->close();

    MessageConsumer consumer1 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TRANSACTED_ROLLBACK_QUEUE,
            sessionAckMode: SESSION_TRANSACTED
        }
    });

    Message? receivedMessage1 = check consumer1->receive(5.0);
    test:assertTrue(receivedMessage1 is Message, "Should receive a message");
    if receivedMessage1 is Message {
        test:assertEquals(receivedMessage1.payload, "Transacted rollback test");
    }

    check consumer1->'rollback();
    check consumer1->close();

    runtime:sleep(1.0);

    MessageConsumer consumer2 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TRANSACTED_ROLLBACK_QUEUE,
            sessionAckMode: SESSION_TRANSACTED
        }
    });

    Message? receivedMessage2 = check consumer2->receive(5.0);
    test:assertTrue(receivedMessage2 is Message, "Message should be redelivered after rollback");
    if receivedMessage2 is Message {
        test:assertEquals(receivedMessage2.payload, "Transacted rollback test");
    }

    check consumer2->'commit();
    check consumer2->close();
}

@test:Config {groups: ["consumer", "transacted"], dependsOn: [testTransactedSessionWithRollback]}
isolated function testTransactedSessionMultipleMessagesCommit() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: TRANSACTED_MULTIPLE_COMMIT_QUEUE}
    });

    check producer->send({payload: "Transacted message 1"});
    check producer->send({payload: "Transacted message 2"});
    check producer->send({payload: "Transacted message 3"});
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TRANSACTED_MULTIPLE_COMMIT_QUEUE,
            sessionAckMode: SESSION_TRANSACTED
        }
    });

    Message? msg1 = check consumer->receive(5.0);
    test:assertTrue(msg1 is Message);
    if msg1 is Message {
        test:assertEquals(msg1.payload, "Transacted message 1");
    }

    Message? msg2 = check consumer->receive(5.0);
    test:assertTrue(msg2 is Message);
    if msg2 is Message {
        test:assertEquals(msg2.payload, "Transacted message 2");
    }

    Message? msg3 = check consumer->receive(5.0);
    test:assertTrue(msg3 is Message);
    if msg3 is Message {
        test:assertEquals(msg3.payload, "Transacted message 3");
    }

    check consumer->'commit();
    check consumer->close();

    MessageConsumer consumer2 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TRANSACTED_MULTIPLE_COMMIT_QUEUE,
            sessionAckMode: SESSION_TRANSACTED
        }
    });

    Message? shouldBeNull = check consumer2->receive(1.0);
    test:assertTrue(shouldBeNull is (), "No messages should be redelivered after commit");
    check consumer2->close();
}

@test:Config {groups: ["consumer", "transacted"], dependsOn: [testTransactedSessionMultipleMessagesCommit]}
isolated function testTransactedSessionMultipleMessagesRollback() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: TRANSACTED_MULTIPLE_ROLLBACK_QUEUE}
    });

    check producer->send({payload: "Rollback message 1"});
    check producer->send({payload: "Rollback message 2"});
    check producer->send({payload: "Rollback message 3"});
    check producer->close();

    MessageConsumer consumer1 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TRANSACTED_MULTIPLE_ROLLBACK_QUEUE,
            sessionAckMode: SESSION_TRANSACTED
        }
    });

    Message? msg1 = check consumer1->receive(5.0);
    test:assertTrue(msg1 is Message);

    Message? msg2 = check consumer1->receive(5.0);
    test:assertTrue(msg2 is Message);

    Message? msg3 = check consumer1->receive(5.0);
    test:assertTrue(msg3 is Message);

    check consumer1->'rollback();
    check consumer1->close();

    runtime:sleep(1.0);

    MessageConsumer consumer2 = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TRANSACTED_MULTIPLE_ROLLBACK_QUEUE,
            sessionAckMode: SESSION_TRANSACTED
        }
    });

    Message? redelivered1 = check consumer2->receive(5.0);
    test:assertTrue(redelivered1 is Message, "First message should be redelivered");
    if redelivered1 is Message {
        test:assertEquals(redelivered1.payload, "Rollback message 1");
    }

    Message? redelivered2 = check consumer2->receive(5.0);
    test:assertTrue(redelivered2 is Message, "Second message should be redelivered");
    if redelivered2 is Message {
        test:assertEquals(redelivered2.payload, "Rollback message 2");
    }

    Message? redelivered3 = check consumer2->receive(5.0);
    test:assertTrue(redelivered3 is Message, "Third message should be redelivered");
    if redelivered3 is Message {
        test:assertEquals(redelivered3.payload, "Rollback message 3");
    }

    check consumer2->'commit();
    check consumer2->close();
}

@test:Config {groups: ["consumer", "transacted"], dependsOn: [testTransactedSessionMultipleMessagesRollback]}
isolated function testTransactedSessionWithTopic() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            topicName: TRANSACTED_TOPIC,
            sessionAckMode: SESSION_TRANSACTED
        }
    });

    runtime:sleep(0.5);

    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {topicName: TRANSACTED_TOPIC}
    });

    Message message = {
        payload: "Transacted topic message"
    };
    check producer->send(message);
    check producer->close();

    Message? receivedMessage = check consumer->receive(5.0);
    test:assertTrue(receivedMessage is Message, "Should receive message from topic");
    if receivedMessage is Message {
        test:assertEquals(receivedMessage.payload, "Transacted topic message");
    }

    check consumer->'commit();
    check consumer->close();
}

@test:Config {groups: ["consumer", "transacted"], dependsOn: [testTransactedSessionWithTopic]}
isolated function testTransactedSessionMixedCommitRollback() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: TRANSACTED_MIXED_QUEUE}
    });

    check producer->send({payload: "Mixed test message 1"});
    check producer->send({payload: "Mixed test message 2"});
    check producer->send({payload: "Mixed test message 3"});
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TRANSACTED_MIXED_QUEUE,
            sessionAckMode: SESSION_TRANSACTED
        }
    });

    Message? msg1 = check consumer->receive(5.0);
    test:assertTrue(msg1 is Message);
    if msg1 is Message {
        test:assertEquals(msg1.payload, "Mixed test message 1");
    }
    check consumer->'commit();

    Message? msg2 = check consumer->receive(5.0);
    test:assertTrue(msg2 is Message);
    if msg2 is Message {
        test:assertEquals(msg2.payload, "Mixed test message 2");
    }
    check consumer->'rollback();

    runtime:sleep(1.0);

    Message? msg2Again = check consumer->receive(5.0);
    test:assertTrue(msg2Again is Message);
    if msg2Again is Message {
        test:assertEquals(msg2Again.payload, "Mixed test message 2");
    }

    Message? msg3 = check consumer->receive(5.0);
    test:assertTrue(msg3 is Message);
    if msg3 is Message {
        test:assertEquals(msg3.payload, "Mixed test message 3");
    }

    check consumer->'commit();
    check consumer->close();
}

@test:Config {groups: ["consumer", "transacted"], dependsOn: [testTransactedSessionMixedCommitRollback]}
isolated function testTransactedSessionWithDifferentMessageTypes() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: TRANSACTED_MSG_TYPES_QUEUE}
    });

    check producer->send({payload: "Transacted text"});
    check producer->send({payload: [10, 20, 30]});
    check producer->send({payload: {"status": "active", "count": 100}});
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TRANSACTED_MSG_TYPES_QUEUE,
            sessionAckMode: SESSION_TRANSACTED
        }
    });

    Message? msg1 = check consumer->receive(5.0);
    test:assertTrue(msg1 is Message);
    if msg1 is Message {
        test:assertTrue(msg1.payload is string);
        test:assertEquals(msg1.payload, "Transacted text");
    }

    Message? msg2 = check consumer->receive(5.0);
    test:assertTrue(msg2 is Message);
    if msg2 is Message {
        test:assertTrue(msg2.payload is byte[]);
    }

    Message? msg3 = check consumer->receive(5.0);
    test:assertTrue(msg3 is Message);
    if msg3 is Message {
        test:assertTrue(msg3.payload is map<Value>);
    }

    check consumer->'commit();
    check consumer->close();
}

@test:Config {groups: ["consumer", "transacted"], dependsOn: [testTransactedSessionWithDifferentMessageTypes]}
isolated function testTransactedProducerAndConsumer() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        destination: {queueName: TRANSACTED_PRODUCER_CONSUMER_QUEUE},
        transacted: true
    });

    check producer->send({payload: "Transacted producer message 1"});
    check producer->send({payload: "Transacted producer message 2"});
    check producer->commit();
    check producer->close();

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: TRANSACTED_PRODUCER_CONSUMER_QUEUE,
            sessionAckMode: SESSION_TRANSACTED
        }
    });

    Message? msg1 = check consumer->receive(5.0);
    test:assertTrue(msg1 is Message);
    if msg1 is Message {
        test:assertEquals(msg1.payload, "Transacted producer message 1");
    }

    Message? msg2 = check consumer->receive(5.0);
    test:assertTrue(msg2 is Message);
    if msg2 is Message {
        test:assertEquals(msg2.payload, "Transacted producer message 2");
    }

    check consumer->commit();
    check consumer->close();
}
