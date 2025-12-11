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
// Client Acknowledgement Mode Tests
// ========================================

@test:Config {groups: ["consumer", "clientack"]}
isolated function testConsumerClientAckInit() returns error? {
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: ACK_SINGLE_QUEUE,
            ackMode: CLIENT_ACK
        }
    });

    check consumer->close();
}

@test:Config {groups: ["consumer", "clientack"], dependsOn: [testConsumerClientAckInit]}
isolated function testConsumerAcknowledge() returns error? {
    // Send message
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: ACK_SINGLE_QUEUE},
        {payload: "Message to ACK".toBytes()}
    );

    check producer->close();

    // Receive and acknowledge
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: ACK_SINGLE_QUEUE,
            ackMode: CLIENT_ACK
        }
    });

    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg is Message, "Should receive a message");

    check consumer->close();
}

@test:Config {groups: ["consumer", "clientack"], dependsOn: [testConsumerClientAckInit]}
isolated function testConsumerAcknowledgeMultiple() returns error? {
    // Send multiple messages
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: ACK_MULTIPLE_QUEUE},
        {payload: "ACK Message 1".toBytes()}
    );

    check producer->send(
        {queueName: ACK_MULTIPLE_QUEUE},
        {payload: "ACK Message 2".toBytes()}
    );

    check producer->send(
        {queueName: ACK_MULTIPLE_QUEUE},
        {payload: "ACK Message 3".toBytes()}
    );

    check producer->close();

    // Receive and acknowledge all
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: ACK_MULTIPLE_QUEUE,
            ackMode: CLIENT_ACK
        }
    });

    Message? msg1 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg1 is Message, "Should receive first message");
    if msg1 is Message {
        check consumer->ack(msg1);
    }

    Message? msg2 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg2 is Message, "Should receive second message");
    if msg2 is Message {
        check consumer->ack(msg2);
    }

    Message? msg3 = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg3 is Message, "Should receive third message");
    if msg3 is Message {
        check consumer->ack(msg3);
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "clientack"], dependsOn: [testConsumerClientAckInit]}
isolated function testConsumerNackWithRequeue() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: NACK_REQUEUE_QUEUE},
        {payload: "Message to NACK with requeue".toBytes()}
    );

    check producer->close();

    // Receive and NACK with requeue
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: NACK_REQUEUE_QUEUE,
            ackMode: CLIENT_ACK
        }
    });

    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is Message, "Should receive a message");

    if msg is Message {
        // NACK with requeue (message will be redelivered)
        check consumer->nack(msg, requeue = true);
    }

    Message? redeliveredMsg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(redeliveredMsg is Message, "Message should be redelivered after NACK with requeue");

    if redeliveredMsg is Message {
        test:assertEquals(redeliveredMsg.payload, "Message to NACK with requeue".toBytes(), "Redelivered message should match");
        // Check redelivered flag
        test:assertTrue(redeliveredMsg.redelivered == true, "Redelivered flag should be set");
        // Acknowledge to clean up
        check consumer->ack(redeliveredMsg);
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "clientack"], dependsOn: [testConsumerClientAckInit]}
isolated function testConsumerNackWithReject() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: NACK_REJECT_QUEUE},
        {payload: "Message to NACK without requeue".toBytes()}
    );

    check producer->close();

    // Receive and NACK without requeue
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: NACK_REJECT_QUEUE,
            ackMode: CLIENT_ACK
        }
    });

    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);

    test:assertTrue(msg is Message, "Should receive a message");

    if msg is Message {
        // NACK without requeue (message rejected, goes to DMQ)
        check consumer->nack(msg, requeue = false);
    }

    // Try to receive again - should timeout (no message)
    Message? noMsg = check consumer->receive(SHORT_RECEIVE_TIMEOUT);

    test:assertTrue(noMsg is (), "Message should not be redelivered after NACK with reject");

    check consumer->close();
}

@test:Config {groups: ["consumer", "clientack"], dependsOn: [testConsumerClientAckInit]}
isolated function testConsumerUnacknowledgedRedelivery() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: ACK_REDELIVERY_QUEUE},
        {payload: "Message for redelivery test".toBytes()}
    );

    check producer->close();

    // First consumer - receive but don't acknowledge, then close
    MessageConsumer consumer1 = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: ACK_REDELIVERY_QUEUE,
            ackMode: CLIENT_ACK
        }
    });

    Message? msg1 = check consumer1->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg1 is Message, "First consumer should receive message");

    // Close without acknowledging
    check consumer1->close();

    // Second consumer - should receive the same message (redelivered)
    MessageConsumer consumer2 = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {
            queueName: ACK_REDELIVERY_QUEUE,
            ackMode: CLIENT_ACK
        }
    });

    Message? msg2 = check consumer2->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg2 is Message, "Second consumer should receive redelivered message");

    if msg2 is Message {
        test:assertEquals(msg2.payload, "Message for redelivery test".toBytes(), "Redelivered message should match");
        test:assertTrue(msg2.redelivered == true, "Redelivered flag should be set");
        // Acknowledge to clean up
        check consumer2->ack(msg2);
    }

    check consumer2->close();
}
