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

type ClientBindingAddress record {|
    string city;
    string country;
|};

type ClientBindingPerson record {|
    string name;
    int age;
    ClientBindingAddress address;
|};

type ClientBindingFlatRecord record {|
    string name;
    int age;
|};

type ClientBindingIncompatibleRecord record {|
    string name;
    int age;
    string requiredExtraField;
|};

isolated function publishToQueue(string queueName, anydata payload) returns error? {
    MessageProducer producer = check new (BROKER_URL, {...connectionConfig()});
    check producer->send({payload}, {queueName});
    check producer->close();
}

isolated function newBindingConsumer(string queueName) returns MessageConsumer|error =>
    new (BROKER_URL, {
    messageVpn: MESSAGE_VPN,
    auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD},
    subscriptionConfig: {queueName}
});

// ========================================
// Positive cases: all run sequentially against the single shared `BINDING_CLIENT_POSITIVE_QUEUE`
// (each test fully publishes, receives, and closes before returning, leaving the queue empty for the
// next case). The `dependsOn` chain makes that ordering explicit.
// ========================================
@test:Config {groups: ["client", "databinding"]}
function testClientBindStringPayload() returns error? {
    check publishToQueue(BINDING_CLIENT_POSITIVE_QUEUE, "hello-client-binding");
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_POSITIVE_QUEUE);
    record {|*Message; string payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; string payload;|}, "Should receive a message");
    if msg is record {|*Message; string payload;|} {
        test:assertEquals(msg.payload, "hello-client-binding");
    }
}

@test:Config {groups: ["client", "databinding"], dependsOn: [testClientBindStringPayload]}
function testClientBindIntPayload() returns error? {
    check publishToQueue(BINDING_CLIENT_POSITIVE_QUEUE, 99);
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_POSITIVE_QUEUE);
    record {|*Message; int payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; int payload;|}, "Should receive a message");
    if msg is record {|*Message; int payload;|} {
        test:assertEquals(msg.payload, 99);
    }
}

@test:Config {groups: ["client", "databinding"], dependsOn: [testClientBindIntPayload]}
function testClientBindFloatPayload() returns error? {
    check publishToQueue(BINDING_CLIENT_POSITIVE_QUEUE, <float>7.25);
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_POSITIVE_QUEUE);
    record {|*Message; float payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; float payload;|}, "Should receive a message");
    if msg is record {|*Message; float payload;|} {
        test:assertEquals(msg.payload, <float>7.25);
    }
}

@test:Config {groups: ["client", "databinding"], dependsOn: [testClientBindFloatPayload]}
function testClientBindDecimalPayload() returns error? {
    check publishToQueue(BINDING_CLIENT_POSITIVE_QUEUE, 5.5d);
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_POSITIVE_QUEUE);
    record {|*Message; decimal payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; decimal payload;|}, "Should receive a message");
    if msg is record {|*Message; decimal payload;|} {
        test:assertEquals(msg.payload, 5.5d);
    }
}

@test:Config {groups: ["client", "databinding"], dependsOn: [testClientBindDecimalPayload]}
function testClientBindBooleanPayload() returns error? {
    check publishToQueue(BINDING_CLIENT_POSITIVE_QUEUE, false);
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_POSITIVE_QUEUE);
    record {|*Message; boolean payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; boolean payload;|}, "Should receive a message");
    if msg is record {|*Message; boolean payload;|} {
        test:assertEquals(msg.payload, false);
    }
}

@test:Config {groups: ["client", "databinding"], dependsOn: [testClientBindBooleanPayload]}
function testClientBindBytesPayload() returns error? {
    check publishToQueue(BINDING_CLIENT_POSITIVE_QUEUE, "client-bytes-binding".toBytes());
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_POSITIVE_QUEUE);
    record {|*Message; byte[] payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; byte[] payload;|}, "Should receive a message");
    if msg is record {|*Message; byte[] payload;|} {
        test:assertEquals(msg.payload, "client-bytes-binding".toBytes());
    }
}

@test:Config {groups: ["client", "databinding"], dependsOn: [testClientBindBytesPayload]}
function testClientBindXmlPayload() returns error? {
    xml orderXml = xml `<order><id>7</id></order>`;
    check publishToQueue(BINDING_CLIENT_POSITIVE_QUEUE, orderXml);
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_POSITIVE_QUEUE);
    record {|*Message; xml payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; xml payload;|}, "Should receive a message");
    if msg is record {|*Message; xml payload;|} {
        test:assertEquals(msg.payload.toString(), orderXml.toString());
    }
}

@test:Config {groups: ["client", "databinding"], dependsOn: [testClientBindXmlPayload]}
function testClientBindJsonPayload() returns error? {
    json orderJson = {name: "client-json-order", tags: ["x", "y"]};
    check publishToQueue(BINDING_CLIENT_POSITIVE_QUEUE, orderJson);
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_POSITIVE_QUEUE);
    record {|*Message; json payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; json payload;|}, "Should receive a message");
    if msg is record {|*Message; json payload;|} {
        test:assertEquals(msg.payload, orderJson);
    }
}

@test:Config {groups: ["client", "databinding"], dependsOn: [testClientBindJsonPayload]}
function testClientBindFlatRecordPayload() returns error? {
    ClientBindingFlatRecord flatRecord = {name: "Dana", age: 22};
    check publishToQueue(BINDING_CLIENT_POSITIVE_QUEUE, flatRecord);
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_POSITIVE_QUEUE);
    record {|*Message; ClientBindingFlatRecord payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; ClientBindingFlatRecord payload;|}, "Should receive a message");
    if msg is record {|*Message; ClientBindingFlatRecord payload;|} {
        test:assertEquals(msg.payload, flatRecord);
    }
}

@test:Config {groups: ["client", "databinding"], dependsOn: [testClientBindFlatRecordPayload]}
function testClientBindNestedRecordPayload() returns error? {
    ClientBindingPerson person = {name: "Erin", age: 35, address: {city: "Kandy", country: "LK"}};
    check publishToQueue(BINDING_CLIENT_POSITIVE_QUEUE, person);
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_POSITIVE_QUEUE);
    record {|*Message; ClientBindingPerson payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; ClientBindingPerson payload;|}, "Should receive a message");
    if msg is record {|*Message; ClientBindingPerson payload;|} {
        test:assertEquals(msg.payload, person);
    }
}

@test:Config {groups: ["client", "databinding"], dependsOn: [testClientBindNestedRecordPayload]}
function testClientBindMapPayload() returns error? {
    map<Value> orderMap = {id: 3, active: false, tags: {k: "v"}};
    check publishToQueue(BINDING_CLIENT_POSITIVE_QUEUE, orderMap);
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_POSITIVE_QUEUE);
    record {|*Message; map<Value> payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; map<Value> payload;|}, "Should receive a message");
    if msg is record {|*Message; map<Value> payload;|} {
        test:assertEquals(msg.payload, orderMap);
    }
}

// ========================================
// Negative cases: each keeps its own dedicated queue (NOT shared with anything else, including the
// positive queue above). A failed data-binding attempt leaves the message unacknowledged, so it may
// be redelivered - sharing a queue would let a poisoned message leak into an unrelated later test.
// ========================================
@test:Config {groups: ["client", "databinding", "negative"]}
function testClientBindMismatchIntSentStringExpected() returns error? {
    check publishToQueue(BINDING_CLIENT_MISMATCH_STRING_QUEUE, 42);
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_MISMATCH_STRING_QUEUE);
    record {|*Message; string payload;|}|Error? result = consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(result is Error, "Binding a BytesMessage to 'string' should fail");
    if result is Error {
        test:assertEquals(result.message(),
                "Data binding failed: Cannot bind BytesMessage to type 'string'. Use TextMessage for string/xml payloads");
    }
}

@test:Config {groups: ["client", "databinding", "negative"]}
function testClientBindMismatchStringSentIntExpected() returns error? {
    check publishToQueue(BINDING_CLIENT_MISMATCH_INT_QUEUE, "not-an-int");
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_MISMATCH_INT_QUEUE);
    record {|*Message; int payload;|}|Error? result = consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(result is Error, "Binding a TextMessage to 'int' should fail");
    if result is Error {
        test:assertEquals(result.message(),
                "Data binding failed: Cannot bind TextMessage to type 'int'. Expected 'string' or 'xml'");
    }
}

@test:Config {groups: ["client", "databinding", "negative"]}
function testClientBindMismatchIncompatibleRecord() returns error? {
    ClientBindingFlatRecord flatRecord = {name: "Frank", age: 50};
    check publishToQueue(BINDING_CLIENT_MISMATCH_RECORD_QUEUE, flatRecord);
    MessageConsumer consumer = check newBindingConsumer(BINDING_CLIENT_MISMATCH_RECORD_QUEUE);
    record {|*Message; ClientBindingIncompatibleRecord payload;|}|Error? result =
        consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(result is Error, "Binding to a structurally incompatible record should fail");
    if result is Error {
        test:assertTrue(result.message().startsWith("Data binding failed: "),
                "Error message should indicate a data binding failure");
    }
}
