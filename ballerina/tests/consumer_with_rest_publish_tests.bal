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

import ballerina/test;
import ballerina/http;
import ballerina/io;

final http:Client solaceRest = check new ("localhost:9000/QUEUE");

@test:Config {
    groups: ["consumer", "REST", "firstTest"]
}
isolated function testReceiveJsonPayloads() returns error? {
    MessageConsumer consumer = check createQueueConsumer("jsonQueue");
    json payload = {
        "message": "This is a sample message"
    };
    
    http:Response _ = check solaceRest->/jsonQueue.post(payload);
    Message? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    test:assertTrue(msg is Message, "Should receive a message");
    if msg is () {
        return;
    }

    string payloadStr = check string:fromBytes(msg.payload);
    io:println("Received payload:  ", payloadStr);
    // The Solace REST API may add prefix characters, so we need to extract the JSON portion
    int? jsonStartNullable = payloadStr.indexOf("{");
    if jsonStartNullable is () {
        test:assertFail("No JSON object found in payload");
    }
    int jsonStart = jsonStartNullable;
    string jsonStr = payloadStr.substring(jsonStart);
    json receivedPayload = check jsonStr.fromJsonString();
    test:assertEquals(receivedPayload, payload, "Received payload is different");
}

@test:Config {
    groups: ["consumer", "REST"]
}
isolated function testReceiveTextPayloads() returns error? {}

@test:Config {
    groups: ["consumer", "REST"]
}
isolated function testReceiveXmlPayloads() returns error? {}

@test:Config {
    groups: ["consumer", "REST"]
}
isolated function testReceiveBinaryPayload() returns error? {}

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
