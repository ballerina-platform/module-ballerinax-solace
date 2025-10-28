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

final MessageProducer databindingProducer = check new (BROKER_URL, {
    messageVpn: MESSAGE_VPN,
    enableDynamicDurables: true,
    auth: {
        username: BROKER_USERNAME,
        password: BROKER_PASSWORD
    },
    destination: {queueName: DATABINDING_QUEUE}
});

final MessageConsumer databindingConsumer = check new (BROKER_URL, {
    messageVpn: MESSAGE_VPN,
    enableDynamicDurables: true,
    auth: {
        username: BROKER_USERNAME,
        password: BROKER_PASSWORD
    },
    subscriptionConfig: {
        queueName: DATABINDING_QUEUE
    }
});

@test:Config {groups: ["dataBinding"]}
isolated function testStringDatabinding() returns error? {
    string payload = "This is a sample payload";
    check databindingProducer->send({payload});

    record {|*Message; string payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed payloads differ");
}

@test:Config {groups: ["dataBinding"], dependsOn: [testStringDatabinding]}
isolated function testXmlDatabinding() returns error? {
    xml payload = xml `<order><id>67890</id><item>Gadget</item><quantity>25</quantity></order>`;
    check databindingProducer->send({payload});

    record {|*Message; xml payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed payloads differ");
}

@test:Config {groups: ["dataBinding"], dependsOn: [testXmlDatabinding]}
isolated function testJsonDatabinding() returns error? {
    json payload = {
        name: "John Wick",
        age: 35,
        sex: "Male"
    };
    check databindingProducer->send({payload});

    record {|*Message; json payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed payloads differ");
}

@test:Config {groups: ["dataBinding", "recordDatabinding"], dependsOn: [testJsonDatabinding]}
isolated function testRecordDatabinding() returns error? {
    record {
        string name;
        int age;
        string sex;
    } payload = {
        name: "John Wick",
        age: 35,
        sex: "Male"
    };
    check databindingProducer->send({payload});

    record {|
        *Message;
        record {|
            string name;
            int age;
            string sex;
        |} payload;
    |}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed payloads differ");
}

@test:Config {groups: ["dataBinding", "recordDatabinding"], dependsOn: [testRecordDatabinding]}
isolated function testRecordDatabindingUsinMapMessage() returns error? {
    record {|
        string name;
        int age;
        string sex;
    |} payload = {
        name: "John Wick",
        age: 35,
        sex: "Male"
    };
    check databindingProducer->send({payload});

    record {|
        *Message;
        record {|
            string name;
            int age;
            string sex;
        |} payload;
    |}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed payloads differ");
}

@test:Config {groups: ["dataBinding"], dependsOn: [testRecordDatabindingUsinMapMessage]}
isolated function testMapDatabinding() returns error? {
    map<Value> payload = {
        name: "John Wick",
        age: 35,
        sex: "Male"
    };
    check databindingProducer->send({payload});

    record {|
        *Message;
        map<Value> payload;
    |}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed payloads differ");    
}
