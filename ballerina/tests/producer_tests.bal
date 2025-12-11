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
// Producer Initialization Tests
// ========================================

@test:Config {groups: ["producer", "init"]}
isolated function testProducerInitWithQueue() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    boolean isClosed = producer->isClosed();
    test:assertFalse(isClosed, "Producer should not be closed after init");

    check producer->close();

    boolean isClosedAfter = producer->isClosed();
    test:assertTrue(isClosedAfter, "Producer should be closed after close");
}

@test:Config {groups: ["producer", "init"]}
isolated function testProducerInitWithCompression() returns error? {
    MessageProducer producer = check new (BROKER_URL_COMPRESSED, {
        vpnName: MESSAGE_VPN,
        compressionLevel: 6,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    boolean isClosed = producer->isClosed();
    test:assertFalse(isClosed, "Producer with compression should not be closed after init");

    check producer->close();
}

@test:Config {groups: ["producer", "init"]}
isolated function testProducerInitWithClientName() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        clientName: "test-producer-client",
        clientDescription: "Test producer for Ballerina Solace SMF",
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    boolean isClosed = producer->isClosed();
    test:assertFalse(isClosed, "Producer with custom client name should not be closed after init");

    check producer->close();
}

@test:Config {groups: ["producer", "init", "negative"]}
isolated function testProducerInitInvalidUrl() returns error? {
    MessageProducer|error producer = new ("invalid-url", {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    test:assertTrue(producer is error, "Producer init with invalid URL should return error");
}

@test:Config {groups: ["producer", "init"]}
isolated function testProducerInitWithTimeouts() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        connectionTimeout: 20.0,
        readTimeout: 5.0,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->close();
}

// ========================================
// Producer Send Tests
// ========================================

@test:Config {groups: ["producer", "send"], dependsOn: [testProducerInitWithQueue]}
isolated function testProducerSendTextToQueue() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: PRODUCER_TEXT_QUEUE},
        {payload: TEXT_MESSAGE_CONTENT.toBytes()}
    );

    check producer->close();
}

@test:Config {groups: ["producer", "send"], dependsOn: [testProducerInitWithQueue]}
isolated function testProducerSendTextToTopic() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {topicName: PRODUCER_TOPIC},
        {payload: TEXT_MESSAGE_CONTENT.toBytes()}
    );

    check producer->close();
}

@test:Config {groups: ["producer", "send"], dependsOn: [testProducerInitWithQueue]}
isolated function testProducerSendWithProperties() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: PRODUCER_PROPERTIES_QUEUE},
        {
        payload: TEXT_MESSAGE_CONTENT.toBytes(),
        properties: {
            "orderType": "URGENT",
            "priority": 5,
            "customerId": "12345",
            "isProcessed": false
        }
    }
    );

    check producer->close();
}

@test:Config {groups: ["producer", "send"], dependsOn: [testProducerInitWithQueue]}
isolated function testProducerSendWithMetadata() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: PRODUCER_METADATA_QUEUE},
        {
        payload: TEXT_MESSAGE_CONTENT.toBytes(),
        correlationId: "corr-123",
        applicationMessageId: "app-msg-456",
        applicationMessageType: "ORDER_CREATED",
        senderId: "sender-789",
        replyTo: {queueName: "reply/queue"}
    }
    );

    check producer->close();
}

@test:Config {groups: ["producer", "send"], dependsOn: [testProducerInitWithQueue]}
isolated function testProducerSendWithTTL() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: PRODUCER_TTL_QUEUE},
        {
        payload: TEXT_MESSAGE_CONTENT.toBytes(),
        timeToLive: 60000,
        deliveryMode: PERSISTENT
    }
    );

    check producer->close();
}

@test:Config {groups: ["producer", "send"], dependsOn: [testProducerInitWithQueue]}
isolated function testProducerSendPersistentMessage() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: PRODUCER_PERSISTENT_QUEUE},
        {
        payload: TEXT_MESSAGE_CONTENT.toBytes(),
        deliveryMode: PERSISTENT,
        priority: 128
    }
    );

    check producer->close();
}

@test:Config {groups: ["producer", "send"], dependsOn: [testProducerInitWithQueue]}
isolated function testProducerSendWithUserData() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    byte[] userData = [1, 2, 3, 4, 5];

    check producer->send(
        {queueName: PRODUCER_USERDATA_QUEUE},
        {
        payload: TEXT_MESSAGE_CONTENT.toBytes(),
        userData: userData
    }
    );

    check producer->close();
}

// ========================================
// Producer Transaction Tests
// ========================================

@test:Config {groups: ["producer", "transacted"]}
isolated function testProducerTransactedInit() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    boolean isClosed = producer->isClosed();
    test:assertFalse(isClosed, "Transacted producer should not be closed after init");

    check producer->close();
}

@test:Config {groups: ["producer", "transacted"], dependsOn: [testProducerTransactedInit]}
isolated function testProducerTransactedCommit() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    // Send multiple messages (buffered in transaction)
    // Note: Transacted messages must use PERSISTENT delivery mode
    check producer->send(
        {queueName: PRODUCER_TX_COMMIT_QUEUE},
        {
        payload: "Message 1".toBytes(),
        deliveryMode: PERSISTENT
    }
    );

    check producer->send(
        {queueName: PRODUCER_TX_COMMIT_QUEUE},
        {
        payload: "Message 2".toBytes(),
        deliveryMode: PERSISTENT
    }
    );

    // Commit transaction
    check producer->'commit();

    check producer->close();
}

@test:Config {groups: ["producer", "transacted"], dependsOn: [testProducerTransactedInit]}
isolated function testProducerTransactedRollback() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    // Send messages (buffered in transaction)
    // Note: Transacted messages must use PERSISTENT delivery mode
    check producer->send(
        {queueName: PRODUCER_TX_ROLLBACK_QUEUE},
        {
        payload: "Message to rollback".toBytes(),
        deliveryMode: PERSISTENT
    }
    );

    // Rollback transaction (messages discarded)
    check producer->'rollback();

    check producer->close();
}

@test:Config {groups: ["producer", "transacted", "negative"]}
isolated function testProducerCommitWithoutTransaction() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: false, // Non-transacted mode
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    // Attempt to commit without transaction
    error? result = producer->'commit();

    test:assertTrue(result is error, "Commit without transaction should return error");

    check producer->close();
}

@test:Config {groups: ["producer", "transacted"], dependsOn: [testProducerTransactedInit]}
isolated function testProducerTransactedMultipleCommits() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    // First transaction
    check producer->send(
        {queueName: PRODUCER_TX_MULTIPLE_QUEUE},
        {
        payload: "TX1 Message 1".toBytes(),
        deliveryMode: PERSISTENT
    }
    );
    check producer->'commit();

    // Second transaction
    check producer->send(
        {queueName: PRODUCER_TX_MULTIPLE_QUEUE},
        {
        payload: "TX2 Message 1".toBytes(),
        deliveryMode: PERSISTENT
    }
    );
    check producer->send(
        {queueName: PRODUCER_TX_MULTIPLE_QUEUE},
        {
        payload: "TX2 Message 2".toBytes(),
        deliveryMode: PERSISTENT
    }
    );
    check producer->'commit();

    // Third transaction
    check producer->send(
        {queueName: PRODUCER_TX_MULTIPLE_QUEUE},
        {
        payload: "TX3 Message 1".toBytes(),
        deliveryMode: PERSISTENT
    }
    );
    check producer->'commit();

    check producer->close();
}

// ========================================
// Producer Configuration Tests
// ========================================

@test:Config {groups: ["producer", "config"]}
isolated function testProducerWithGenerateTimestamps() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        generateSendTimestamps: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: PRODUCER_TEXT_QUEUE},
        {payload: TEXT_MESSAGE_CONTENT.toBytes()}
    );

    check producer->close();
}

@test:Config {groups: ["producer", "config"]}
isolated function testProducerWithGenerateSequenceNumbers() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        generateSequenceNumbers: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: PRODUCER_TEXT_QUEUE},
        {payload: "Seq Message 1".toBytes()}
    );

    check producer->send(
        {queueName: PRODUCER_TEXT_QUEUE},
        {payload: "Seq Message 2".toBytes()}
    );

    check producer->close();
}

@test:Config {groups: ["producer", "config"]}
isolated function testProducerWithCalculateMessageExpiration() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        calculateMessageExpiration: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {queueName: PRODUCER_TTL_QUEUE},
        {
        payload: TEXT_MESSAGE_CONTENT.toBytes(),
        timeToLive: 30000,
        deliveryMode: PERSISTENT
    }
    );

    check producer->close();
}
