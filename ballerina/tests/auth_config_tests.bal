// Copyright (c) 2026 WSO2 LLC. (http://www.wso2.org).
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

import ballerina/http;
import ballerina/test;

// ========================================
// Authentication Configuration Tests
// ========================================

@test:Config {groups: ["consumer", "auth", "negative"]}
isolated function testConsumerOidcTokenAuthIsRecognized() returns error? {
    MessageConsumer|error consumer = new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            issuer: "https://example.com",
            oidcToken: "dummy-oidc-token"
        },
        subscriptionConfig: {queueName: CONSUMER_INIT_QUEUE}
    });

    test:assertTrue(consumer is error, "Init with OIDC auth over a non-TLS connection should fail");
    if consumer is error {
        string message = consumer.message();
        test:assertTrue(message.includes("OAuth"),
                "Failure should be OAuth2-related, proving the oidcToken config was recognized: " + message);
        test:assertFalse(message.includes("username"),
                "Failure must not be a Basic-auth username error - that would mean the OIDC " +
                "config was silently discarded: " + message);
    }
}

// Companion check for the sibling OAuth2AccessTokenAuth variant, which shares the same
// createAuthConfig() branch that was fixed for the OIDC case.
@test:Config {groups: ["consumer", "auth", "negative"]}
isolated function testConsumerOAuth2AccessTokenAuthIsRecognized() returns error? {
    MessageConsumer|error consumer = new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            issuer: "https://example.com",
            accessToken: "dummy-access-token"
        },
        subscriptionConfig: {queueName: CONSUMER_INIT_QUEUE}
    });

    test:assertTrue(consumer is error, "Init with OAuth2 access-token auth over a non-TLS connection should fail");
    if consumer is error {
        test:assertTrue(consumer.message().includes("OAuth"),
                "Failure should be OAuth2-related: " + consumer.message());
    }
}

// ========================================
// Basic Auth Tests
// ========================================

@test:Config {groups: ["consumer", "auth"]}
isolated function testConsumerBasicAuth() returns error? {
    check sendMessageToQueue(CONSUMER_BASIC_AUTH_QUEUE, "Basic Auth Message");

    MessageConsumer consumer = check new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName: CONSUMER_BASIC_AUTH_QUEUE}
    });

    BytesPayloadMessage? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg is BytesPayloadMessage, "Should receive a message over Basic auth");
    if msg is BytesPayloadMessage {
        test:assertEquals(msg.payload, "Basic Auth Message".toBytes(), "Payload should round-trip correctly");
    }

    check consumer->close();
}

// ========================================
// TLS Tests
// ========================================

isolated function sendMessageToQueueTls(string queueName, string content) returns error? {
    MessageProducer producer = check new (BROKER_URL_TLS, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        secureSocket: {
            trustStore: {location: CLIENT_TRUSTSTORE_PATH, password: CLIENT_TRUSTSTORE_PASSWORD, format: PKCS12}
        }
    });

    check producer->send(
        {payload: content.toBytes()},
        {queueName: queueName}
    );

    check producer->close();
}

@test:Config {groups: ["consumer", "auth", "tls"]}
isolated function testConsumerBasicAuthOverTls() returns error? {
    check sendMessageToQueueTls(CONSUMER_TLS_QUEUE, "TLS Basic Auth Message");

    MessageConsumer consumer = check new (BROKER_URL_TLS, {
        messageVpn: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        secureSocket: {
            trustStore: {location: CLIENT_TRUSTSTORE_PATH, password: CLIENT_TRUSTSTORE_PASSWORD, format: PKCS12}
        },
        subscriptionConfig: {queueName: CONSUMER_TLS_QUEUE}
    });

    BytesPayloadMessage? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg is BytesPayloadMessage, "Should receive a message over a TLS-secured connection");
    if msg is BytesPayloadMessage {
        test:assertEquals(msg.payload, "TLS Basic Auth Message".toBytes(), "Payload should round-trip correctly");
    }

    check consumer->close();
}

// ========================================
// OAuth2 / OIDC Tests (positive, over TLS against the mock IdP)
// ========================================

final http:Client mockIdpClient = check new (MOCK_IDP_URL);

// Fetches a real signed JWT from the mock IdP for use as an OAuth2 access token or an OIDC
// ID token (the connector/broker only care that it's a valid signed JWT with the right claims -
// not which OAuth flow produced it - see the OAuth2 spike notes in the implementation plan).
//
// The "Host: mock-idp:8080" header makes the mock IdP report its issuer/JWKS URLs as
// OAUTH_ISSUER (the docker-network-internal identity the Solace broker independently resolves
// when it fetches JWKS), even though this request physically hits the host-mapped port.
// Without this, the token's "iss" claim would be host-facing and would never match what the
// broker validates against.
isolated function fetchOauthToken() returns string|error {
    string body = "grant_type=client_credentials&client_id=test-client&client_secret=whatever&scope=solace";
    json response = check mockIdpClient->/default/token.post(
        body,
        headers = {"Host": OAUTH_HOST_HEADER},
        mediaType = "application/x-www-form-urlencoded"
    );
    return (check response.access_token).toString();
}

@test:Config {groups: ["consumer", "auth", "oauth"]}
isolated function testConsumerOAuth2AccessTokenOverTls() returns error? {
    string token = check fetchOauthToken();
    check sendMessageToQueueTls(CONSUMER_OAUTH_ACCESS_QUEUE, "OAuth2 Access Token Message");

    MessageConsumer consumer = check new (BROKER_URL_TLS, {
        messageVpn: MESSAGE_VPN,
        auth: {issuer: OAUTH_ISSUER, accessToken: token},
        secureSocket: {
            trustStore: {location: CLIENT_TRUSTSTORE_PATH, password: CLIENT_TRUSTSTORE_PASSWORD, format: PKCS12}
        },
        subscriptionConfig: {queueName: CONSUMER_OAUTH_ACCESS_QUEUE}
    });

    BytesPayloadMessage? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg is BytesPayloadMessage, "Should receive a message over OAuth2 access-token auth");
    if msg is BytesPayloadMessage {
        test:assertEquals(msg.payload, "OAuth2 Access Token Message".toBytes(), "Payload should round-trip correctly");
    }

    check consumer->close();
}

@test:Config {groups: ["consumer", "auth", "oauth"]}
isolated function testConsumerOidcTokenAuthOverTls() returns error? {
    string token = check fetchOauthToken();
    check sendMessageToQueueTls(CONSUMER_OAUTH_OIDC_QUEUE, "OIDC ID Token Message");

    MessageConsumer consumer = check new (BROKER_URL_TLS, {
        messageVpn: MESSAGE_VPN,
        auth: {issuer: OAUTH_ISSUER, oidcToken: token},
        secureSocket: {
            trustStore: {location: CLIENT_TRUSTSTORE_PATH, password: CLIENT_TRUSTSTORE_PASSWORD, format: PKCS12}
        },
        subscriptionConfig: {queueName: CONSUMER_OAUTH_OIDC_QUEUE}
    });

    BytesPayloadMessage? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg is BytesPayloadMessage, "Should receive a message over OIDC ID-token auth");
    if msg is BytesPayloadMessage {
        test:assertEquals(msg.payload, "OIDC ID Token Message".toBytes(), "Payload should round-trip correctly");
    }

    check consumer->close();
}

// ========================================
// Kerberos Auth Tests
// ========================================

@test:Config {groups: ["consumer", "auth", "negative"]}
isolated function testConsumerKerberosAuthIsRecognized() returns error? {
    MessageConsumer|error consumer = new (BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        auth: {
            serviceName: "solace",
            jaasLoginContext: "SolaceGSS",
            mutualAuthentication: false
        },
        subscriptionConfig: {queueName: CONSUMER_INIT_QUEUE}
    });

    test:assertTrue(consumer is error, "Init with Kerberos auth against a broker with no GSS-KRB profile should fail");
}
