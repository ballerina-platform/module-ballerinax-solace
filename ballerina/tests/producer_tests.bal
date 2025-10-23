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

@test:Config {
    groups: ["producer"]
}
isolated function testProducerInitWithQueue() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });
    check producer->close();
}

@test:Config {
    groups: ["producer"]
}
isolated function testProducerInitWithTopic() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {topicName: TEST_TOPIC},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });
    check producer->close();
}

@test:Config {
    groups: ["producer"]
}
isolated function testSendTextMessage() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    Message message = {
        payload: TEXT_MESSAGE_CONTENT
    };
    check producer->send(message);
    check producer->close();
}

@test:Config {
    groups: ["producer"]
}
isolated function testSendBytesMessage() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    byte[] byteContent = TEXT_MESSAGE_CONTENT.toBytes();
    Message message = {
        payload: byteContent
    };
    check producer->send(message);
    check producer->close();
}

@test:Config {
    groups: ["producer"]
}
isolated function testSendMapMessage() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    map<Value> mapContent = {
        "name": "John Doe",
        "age": 30,
        "active": true,
        "salary": 75000.50
    };
    Message message = {
        payload: mapContent
    };
    check producer->send(message);
    check producer->close();
}

@test:Config {
    groups: ["producer"]
}
isolated function testSendMessageWithProperties() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    Message message = {
        payload: TEXT_MESSAGE_CONTENT,
        properties: {
            "priority": 5,
            "source": "ballerina-test",
            "urgent": true
        }
    };
    check producer->send(message);
    check producer->close();
}

@test:Config {
    groups: ["producer"]
}
isolated function testSendMessageWithCorrelationId() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    Message message = {
        payload: TEXT_MESSAGE_CONTENT,
        correlationId: "test-correlation-id-12345"
    };
    check producer->send(message);
    check producer->close();
}

@test:Config {
    groups: ["producer"]
}
isolated function testSendMessageWithJmsType() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    Message message = {
        payload: TEXT_MESSAGE_CONTENT,
        jmsType: "OrderMessage"
    };
    check producer->send(message);
    check producer->close();
}

@test:Config {
    groups: ["producer"]
}
isolated function testSendToTopic() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {topicName: TEST_TOPIC},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    Message message = {
        payload: TEXT_MESSAGE_CONTENT
    };
    check producer->send(message);
    check producer->close();
}

@test:Config {
    groups: ["producer", "transacted"]
}
isolated function testTransactedProducerCommit() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_TRANSACTED_QUEUE},
        messageVpn: MESSAGE_VPN,
        transacted: true,
        enableDynamicDurables: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    Message message1 = {
        payload: TEXT_MESSAGE_CONTENT
    };
    check producer->send(message1);

    Message message2 = {
        payload: TEXT_MESSAGE_CONTENT_2
    };
    check producer->send(message2);

    check producer->'commit();
    check producer->close();
}

@test:Config {
    groups: ["producer", "transacted"]
}
isolated function testTransactedProducerRollback() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_TRANSACTED_QUEUE},
        messageVpn: MESSAGE_VPN,
        transacted: true,
        directTransport: false,
        directOptimized: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    Message message = {
        payload: TEXT_MESSAGE_CONTENT
    };
    check producer->send(message);

    check producer->'rollback();
    check producer->close();
}

@test:Config {
    groups: ["producer", "config"]
}
isolated function testProducerWithCustomClientId() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        clientId: "test-client-123",
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });
    check producer->close();
}

@test:Config {
    groups: ["producer", "config"]
}
isolated function testProducerWithCompression() returns error? {
    MessageProducer producer = check new (BROKER_URL_COMPRESSED, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        compressionLevel: 5,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    Message message = {
        payload: TEXT_MESSAGE_CONTENT
    };
    check producer->send(message);
    check producer->close();
}

@test:Config {
    groups: ["producer", "config"]
}
isolated function testProducerWithRetryConfig() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        retryConfig: {
            connectRetries: 3,
            connectRetriesPerHost: 1,
            reconnectRetries: 5,
            reconnectRetryWait: 1.0
        }
    });
    check producer->close();
}

@test:Config {
    groups: ["producer", "negative"]
}
isolated function testProducerInitWithInvalidUrl() {
    MessageProducer|Error producer = new ("invalid-url", {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });
    test:assertTrue(producer is Error, "Expected error for invalid URL");
}

@test:Config {
    groups: ["producer", "negative"],
    enable: false
}
isolated function testProducerInitWithInvalidCredentials() {
    MessageProducer|Error producer = new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: "invalid-user",
            password: "invalid-password"
        }
    });
    test:assertTrue(producer is Error, "Expected error for invalid credentials");
}

@test:Config {
    groups: ["producer", "negative"]
}
isolated function testCommitWithoutTransaction() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        transacted: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    Error? commitResult = producer->'commit();
    test:assertTrue(commitResult is Error, "Expected error when calling commit without transaction");
    check producer->close();
}

@test:Config {
    groups: ["producer", "negative"]
}
isolated function testRollbackWithoutTransaction() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        transacted: false,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    Error? rollbackResult = producer->'rollback();
    test:assertTrue(rollbackResult is Error, "Expected error when calling rollback without transaction");
    check producer->close();
}

@test:Config {
    groups: ["producer", "validation"]
}
isolated function testProducerValidationWithInvalidCompressionLevel() {
    MessageProducer|Error producer = new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        compressionLevel: 15,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });
    test:assertTrue(producer is Error, "Expected validation error for compression level > 9");
    if producer is Error {
        test:assertTrue(producer.message().toLowerAscii().includes("compression"),
                "Error message should mention compression");
    }
}

@test:Config {
    groups: ["producer", "validation"]
}
isolated function testProducerValidationWithLongUsername() {
    MessageProducer|Error producer = new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: "this-is-a-very-long-username-that-exceeds-the-maximum-allowed-length",
            password: BROKER_PASSWORD
        }
    });
    test:assertTrue(producer is Error, "Expected validation error for username > 32 chars");
    if producer is Error {
        test:assertTrue(producer.message().toLowerAscii().includes("username"),
                "Error message should mention username");
    }
}

@test:Config {
    groups: ["producer", "xml"]
}
isolated function testSendXmlMessage() returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        destination: {queueName: TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    xml xmlPayload = xml `<order><id>12345</id><item>Widget</item><quantity>10</quantity></order>`;
    Message message = {
        payload: xmlPayload
    };
    check producer->send(message);
    check producer->close();
}
