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

// ============================================
// SSL/TLS Basic Authentication Tests
// ============================================

@test:Config {
    groups: ["ssl"]
}
isolated function testSSLProducerWithBasicAuth() returns error? {
    MessageProducer producer = check new (BROKER_URL_SSL, {
        destination: {queueName: SSL_TEST_QUEUE},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        secureSocket: {
            trustStore: {
                location: TRUSTSTORE_PATH,
                password: TRUSTSTORE_PASSWORD
            }
        }
    });

    Message message = {
        payload: TEXT_MESSAGE_CONTENT
    };
    check producer->send(message);
    check producer->close();
}

@test:Config {
    groups: ["ssl"],
    dependsOn: [testSSLProducerWithBasicAuth]
}
isolated function testSSLConsumerWithBasicAuth() returns error? {
    MessageConsumer consumer = check new (BROKER_URL_SSL, {
        subscriptionConfig: {
            queueName: SSL_TEST_QUEUE,
            sessionAckMode: AUTO_ACKNOWLEDGE
        },
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        secureSocket: {
            trustStore: {
                location: TRUSTSTORE_PATH,
                password: TRUSTSTORE_PASSWORD
            }
        }
    });

    Message? receivedMessage = check consumer->receive(timeout = 5.0);
    test:assertEquals(receivedMessage?.payload, TEXT_MESSAGE_CONTENT);
    check consumer->close();
}

// ============================================
// SSL/TLS Client Certificate Authentication Tests
// ============================================

@test:Config {
    groups: ["ssl", "ssl-client-cert"]
}
isolated function testSSLProducerWithClientCertificate() returns error? {
    MessageProducer producer = check new (BROKER_URL_SSL, {
        destination: {queueName: SSL_CLIENT_CERT_QUEUE},
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        secureSocket: {
            keyStore: {
                location: KEYSTORE_PATH,
                password: KEYSTORE_PASSWORD,
                keyPassword: KEYSTORE_KEY_PASSWORD
            },
            trustStore: {
                location: TRUSTSTORE_PATH,
                password: TRUSTSTORE_PASSWORD
            }
        }
    });

    Message message = {
        payload: "Client certificate authenticated message"
    };
    check producer->send(message);
    check producer->close();
}

@test:Config {
    groups: ["ssl", "ssl-client-cert"],
    dependsOn: [testSSLProducerWithClientCertificate]
}
isolated function testSSLConsumerWithClientCertificate() returns error? {
    MessageConsumer consumer = check new (BROKER_URL_SSL, {
        subscriptionConfig: {
            queueName: SSL_CLIENT_CERT_QUEUE,
            sessionAckMode: AUTO_ACKNOWLEDGE
        },
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        secureSocket: {
            keyStore: {
                location: KEYSTORE_PATH,
                password: KEYSTORE_PASSWORD,
                keyPassword: KEYSTORE_KEY_PASSWORD
            },
            trustStore: {
                location: TRUSTSTORE_PATH,
                password: TRUSTSTORE_PASSWORD
            }
        }
    });

    Message? receivedMessage = check consumer->receive(timeout = 5.0);
    test:assertEquals(receivedMessage?.payload, "Client certificate authenticated message");
    check consumer->close();
}
