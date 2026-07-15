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
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->close();
}

@test:Config {groups: ["producer", "init"]}
isolated function testProducerInitWithCompression() returns error? {
    MessageProducer producer = check new (BROKER_URL_COMPRESSED, {
        messageVpn: MESSAGE_VPN,
        compressionLevel: 6,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->close();
}

@test:Config {groups: ["producer", "init", "validation", "negative"]}
isolated function testProducerInitWithCompressionLevelTooHigh() returns error? {
    MessageProducer|error producer = new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        compressionLevel: 10,
        auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD}
    });

    test:assertTrue(producer is error, "compressionLevel above 9 should fail validation");
    if producer is error {
        test:assertEquals(producer.message(), "ZLIB compression level cannot exceed 9 (maximum compression)");
    }
}

@test:Config {groups: ["producer", "init", "validation", "negative"]}
isolated function testProducerInitWithNegativeCompressionLevel() returns error? {
    MessageProducer|error producer = new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        compressionLevel: -1,
        auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD}
    });

    test:assertTrue(producer is error, "negative compressionLevel should fail validation");
    if producer is error {
        test:assertEquals(producer.message(), "ZLIB compression level must be at least 0 (no compression)");
    }
}

@test:Config {groups: ["producer", "init", "validation", "negative"]}
isolated function testProducerInitWithTooManyTrustedCommonNames() returns error? {
    string[] tooMany = [];
    foreach int i in 0 ..< 17 {
        tooMany.push(string `cn-${i}`);
    }

    MessageProducer|error producer = new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD},
        secureSocket: {trustedCommonNames: tooMany}
    });

    test:assertTrue(producer is error, "trustedCommonNames with more than 16 entries should fail validation");
    if producer is error {
        test:assertEquals(producer.message(), "Trusted common names list cannot exceed 16 entries");
    }
}

@test:Config {groups: ["producer", "init", "validation", "negative"]}
isolated function testProducerInitWithUsernameTooLong() returns error? {
    string tooLongUsername = "";
    foreach int i in 0 ..< 190 {
        tooLongUsername += "a";
    }

    MessageProducer|error producer = new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {username: tooLongUsername, password: BROKER_PASSWORD}
    });

    test:assertTrue(producer is error, "username longer than 189 characters should fail validation");
    if producer is error {
        test:assertEquals(producer.message(), "Username cannot exceed 189 characters");
    }
}

@test:Config {groups: ["producer", "init", "validation", "negative"]}
isolated function testProducerInitWithPasswordTooLong() returns error? {
    string tooLongPassword = "";
    foreach int i in 0 ..< 129 {
        tooLongPassword += "a";
    }

    MessageProducer|error producer = new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {username: BROKER_USERNAME, password: tooLongPassword}
    });

    test:assertTrue(producer is error, "password longer than 128 characters should fail validation");
    if producer is error {
        test:assertEquals(producer.message(), "Password cannot exceed 128 characters");
    }
}

@test:Config {groups: ["producer", "init"]}
isolated function testProducerInitWithClientName() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        clientName: "test-producer-client",
        clientDescription: "Test producer for Ballerina Solace SMF",
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->close();
}

@test:Config {groups: ["producer", "init", "negative"]}
isolated function testProducerInitInvalidUrl() returns error? {
    MessageProducer|error producer = new ("invalid-url", {
        messageVpn: MESSAGE_VPN,
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
        messageVpn: MESSAGE_VPN,
        connectTimeout: 20.0,
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
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {payload: TEXT_MESSAGE_CONTENT.toBytes()},
        {queueName: PRODUCER_TEXT_QUEUE}
    );

    check producer->close();
}

@test:Config {groups: ["producer", "send"], dependsOn: [testProducerInitWithQueue]}
isolated function testProducerSendTextToTopic() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {payload: TEXT_MESSAGE_CONTENT.toBytes()},
        {topicName: PRODUCER_TOPIC}
    );

    check producer->close();
}

@test:Config {groups: ["producer", "send"], dependsOn: [testProducerInitWithQueue]}
isolated function testProducerSendWithProperties() returns error? {
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
        properties: {
            "orderType": "URGENT",
            "priority": 5,
            "customerId": "12345",
            "isProcessed": false
        }
    },
        {queueName: PRODUCER_PROPERTIES_QUEUE}
    );

    check producer->close();
}

@test:Config {groups: ["producer", "send"], dependsOn: [testProducerInitWithQueue]}
isolated function testProducerSendWithMetadata() returns error? {
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
        correlationId: "corr-123",
        messageId: "app-msg-456",
        messageType: "ORDER_CREATED",
        senderId: "sender-789",
        replyTo: {queueName: "reply/queue"}
    },
        {queueName: PRODUCER_METADATA_QUEUE}
    );

    check producer->close();
}

@test:Config {groups: ["producer", "send"], dependsOn: [testProducerInitWithQueue]}
isolated function testProducerSendWithTTL() returns error? {
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
        timeToLive: 60d,
        deliveryMode: PERSISTENT
    },
        {queueName: PRODUCER_TTL_QUEUE}
    );

    check producer->close();
}

@test:Config {groups: ["producer", "send"], dependsOn: [testProducerInitWithQueue]}
isolated function testProducerSendPersistentMessage() returns error? {
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
        deliveryMode: PERSISTENT,
        priority: 7
    },
        {queueName: PRODUCER_PERSISTENT_QUEUE}
    );

    check producer->close();
}

@test:Config {groups: ["producer", "send"], dependsOn: [testProducerInitWithQueue]}
isolated function testProducerSendWithUserData() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    byte[] userData = [1, 2, 3, 4, 5];

    check producer->send(
        {
        payload: TEXT_MESSAGE_CONTENT.toBytes(),
        userData: userData
    },
        {queueName: PRODUCER_USERDATA_QUEUE}
    );

    check producer->close();
}

// ========================================
// Producer Transaction Tests
// ========================================

@test:Config {groups: ["producer", "transacted"]}
isolated function testProducerTransactedInit() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->close();
}

@test:Config {groups: ["producer", "transacted"], dependsOn: [testProducerTransactedInit]}
isolated function testProducerTransactedCommit() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    // Send multiple messages (buffered in transaction)
    // Note: Transacted messages must use PERSISTENT delivery mode
    check producer->send(
        {
        payload: "Message 1".toBytes(),
        deliveryMode: PERSISTENT
    },
        {queueName: PRODUCER_TX_COMMIT_QUEUE}
    );

    check producer->send(
        {
        payload: "Message 2".toBytes(),
        deliveryMode: PERSISTENT
    },
        {queueName: PRODUCER_TX_COMMIT_QUEUE}
    );

    // Commit transaction
    check producer->'commit();

    check producer->close();
}

@test:Config {groups: ["producer", "transacted"], dependsOn: [testProducerTransactedInit]}
isolated function testProducerTransactedRollback() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    // Send messages (buffered in transaction)
    // Note: Transacted messages must use PERSISTENT delivery mode
    check producer->send(
        {
        payload: "Message to rollback".toBytes(),
        deliveryMode: PERSISTENT
    },
        {queueName: PRODUCER_TX_ROLLBACK_QUEUE}
    );

    // Rollback transaction (messages discarded)
    check producer->'rollback();

    check producer->close();
}

@test:Config {groups: ["producer", "transacted", "negative"]}
isolated function testProducerCommitWithoutTransaction() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
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
        messageVpn: MESSAGE_VPN,
        transacted: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    // First transaction
    check producer->send(
        {
        payload: "TX1 Message 1".toBytes(),
        deliveryMode: PERSISTENT
    },
        {queueName: PRODUCER_TX_MULTIPLE_QUEUE}
    );
    check producer->'commit();

    // Second transaction
    check producer->send(
        {
        payload: "TX2 Message 1".toBytes(),
        deliveryMode: PERSISTENT
    },
        {queueName: PRODUCER_TX_MULTIPLE_QUEUE}
    );
    check producer->send(
        {
        payload: "TX2 Message 2".toBytes(),
        deliveryMode: PERSISTENT
    },
        {queueName: PRODUCER_TX_MULTIPLE_QUEUE}
    );
    check producer->'commit();

    // Third transaction
    check producer->send(
        {
        payload: "TX3 Message 1".toBytes(),
        deliveryMode: PERSISTENT
    },
        {queueName: PRODUCER_TX_MULTIPLE_QUEUE}
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
        messageVpn: MESSAGE_VPN,
        generateSendTimestamps: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {payload: TEXT_MESSAGE_CONTENT.toBytes()},
        {queueName: PRODUCER_TEXT_QUEUE}
    );

    check producer->close();
}

@test:Config {groups: ["producer", "config"]}
isolated function testProducerWithGenerateSequenceNumbers() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        generateSequenceNumbers: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check producer->send(
        {payload: "Seq Message 1".toBytes()},
        {queueName: PRODUCER_TEXT_QUEUE}
    );

    check producer->send(
        {payload: "Seq Message 2".toBytes()},
        {queueName: PRODUCER_TEXT_QUEUE}
    );

    check producer->close();
}

