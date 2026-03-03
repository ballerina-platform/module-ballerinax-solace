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

final http:Client solaceRest = check new ("localhost:9000");

@test:Config {
    groups: ["consumer", "REST"]
}
isolated function testReceiveJsonPayloads() returns error? {
    MessageConsumer consumer = check createQueueConsumer("jsonQueue");
    json payload = {
        "message": "This is a sample message"
    };

    http:Response _ = check solaceRest->/QUEUE/jsonQueue.post(payload, mediaType = "application/json; charset=utf-8");
    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg is Message, "Should receive a message");
    if msg is () {
        return;
    }

    string payloadStr = check string:fromBytes(msg.payload);
    json receivedPayload = check payloadStr.fromJsonString();
    test:assertEquals(receivedPayload, payload, "Received payload is different");
}

@test:Config {
    groups: ["consumer", "REST"]
}
isolated function testReceiveTextPayloads() returns error? {
    MessageConsumer consumer = check createQueueConsumer("textQueue");
    string payload = "Hello, Solace!";

    http:Response _ = check solaceRest->/QUEUE/textQueue.post(payload, mediaType = "text/plain; charset=utf-8");
    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg is Message, "Should receive a message");
    if msg is () {
        return;
    }

    string receivedPayload = check string:fromBytes(msg.payload);
    test:assertEquals(receivedPayload, payload, "Received payload is different");
}

@test:Config {
    groups: ["consumer", "REST"]
}
isolated function testReceiveXmlPayloads() returns error? {
    MessageConsumer consumer = check createQueueConsumer("xmlQueue");
    xml payload = xml `<message><content>Hello, Solace!</content></message>`;

    http:Response _ = check solaceRest->/QUEUE/xmlQueue.post(payload, mediaType = "application/xml; charset=utf-8");
    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg is Message, "Should receive a message");
    if msg is () {
        return;
    }

    string payloadStr = check string:fromBytes(msg.payload);
    xml receivedPayload = check xml:fromString(payloadStr);
    test:assertEquals(receivedPayload, payload, "Received payload is different");
}

@test:Config {
    groups: ["consumer", "REST"]
}
isolated function testReceiveBinaryPayload() returns error? {
    MessageConsumer consumer = check createQueueConsumer("binaryQueue");
    byte[] payload = "Hello".toBytes();

    http:Response _ = check solaceRest->/QUEUE/binaryQueue.post(payload, mediaType = "application/octet-stream");
    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg is Message, "Should receive a message");
    if msg is () {
        return;
    }

    test:assertEquals(msg.payload, payload, "Received payload is different");
}

@test:Config {
    groups: ["consumer", "REST", "negative"]
}
isolated function testReceiveInvalidJsonPayload() returns error? {
    MessageConsumer consumer = check createQueueConsumer("invalidJsonQueue");
    string payload = "This is not valid JSON";

    http:Response _ = check solaceRest->/QUEUE/invalidJsonQueue.post(payload, mediaType = "application/json; charset=utf-8");
    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg is Message, "Should receive a message");
    if msg is () {
        return;
    }

    string payloadStr = check string:fromBytes(msg.payload);
    json|error receivedPayload = payloadStr.fromJsonString();
    test:assertTrue(receivedPayload is error, "Should fail to parse invalid JSON payload");
}

@test:Config {
    groups: ["consumer", "REST", "negative"]
}
isolated function testReceiveTimeoutWhenNothingPublished() returns error? {
    MessageConsumer consumer = check createQueueConsumer("emptyRestQueue");

    Message? msg = check consumer->receive(SHORT_RECEIVE_TIMEOUT);
    test:assertTrue(msg is (), "Should return null when no message is published");
}

isolated function createQueueConsumer(string queueName) returns MessageConsumer|error {
    return new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        },
        subscriptionConfig: {queueName}
    });
}
