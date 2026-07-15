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

// Producer -> consumer round-trip tests focused on wire-shape fidelity for each of the three concrete
// wire shapes native code understands (TextMessage / BytesMessage / MapMessage), complementing the
// broader type-matrix coverage in `client_data_binding_tests.bal`.
//
// All tests here are positive round-trips, so they all share the single `BINDING_PRODUCER_QUEUE`,
// chained via `dependsOn` to guarantee they never run concurrently against it.

import ballerina/test;

type ProducerBindingOrder record {|
    string id;
    int quantity;
    boolean expedited;
|};

@test:Config {groups: ["producer", "databinding"]}
function testProducerRoundTripString() returns error? {
    string payload = "round-trip-\u{00e9}\u{00fc}\u{4e2d}\u{6587}"; // includes non-ASCII/unicode content
    check publishToQueue(BINDING_PRODUCER_QUEUE, payload);

    MessageConsumer consumer = check newBindingConsumer(BINDING_PRODUCER_QUEUE);
    record {|*Message; string payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; string payload;|}, "Should receive a message");
    if msg is record {|*Message; string payload;|} {
        test:assertEquals(msg.payload, payload, "Unicode string payload should round-trip via TextMessage");
    }
}

@test:Config {groups: ["producer", "databinding"], dependsOn: [testProducerRoundTripString]}
function testProducerRoundTripMapWithNestedAndBytes() returns error? {
    map<Value> payload = {
        orderId: "ORD-100",
        quantity: 5,
        metadata: {region: "APAC", priority: "high"},
        signature: "sig-bytes".toBytes()
    };
    check publishToQueue(BINDING_PRODUCER_QUEUE, payload);

    MessageConsumer consumer = check newBindingConsumer(BINDING_PRODUCER_QUEUE);
    record {|*Message; map<Value> payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; map<Value> payload;|}, "Should receive a message");
    if msg is record {|*Message; map<Value> payload;|} {
        test:assertEquals(msg.payload["orderId"], "ORD-100");
        test:assertEquals(msg.payload["quantity"], 5);
        test:assertEquals(msg.payload["metadata"], {region: "APAC", priority: "high"});
        test:assertEquals(msg.payload["signature"], "sig-bytes".toBytes());
    }
}

@test:Config {groups: ["producer", "databinding"], dependsOn: [testProducerRoundTripMapWithNestedAndBytes]}
function testProducerRoundTripBytes() returns error? {
    byte[] payload = [0, 1, 2, 255, 128, 64];
    check publishToQueue(BINDING_PRODUCER_QUEUE, payload);

    MessageConsumer consumer = check newBindingConsumer(BINDING_PRODUCER_QUEUE);
    record {|*Message; byte[] payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; byte[] payload;|}, "Should receive a message");
    if msg is record {|*Message; byte[] payload;|} {
        test:assertEquals(msg.payload, payload, "Raw byte[] payload should round-trip via BytesMessage");
    }
}

@test:Config {groups: ["producer", "databinding"], dependsOn: [testProducerRoundTripBytes]}
function testProducerRoundTripXml() returns error? {
    xml payload = xml `<order id="1"><item>widget</item></order>`;
    check publishToQueue(BINDING_PRODUCER_QUEUE, payload);

    MessageConsumer consumer = check newBindingConsumer(BINDING_PRODUCER_QUEUE);
    record {|*Message; xml payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; xml payload;|}, "Should receive a message");
    if msg is record {|*Message; xml payload;|} {
        test:assertEquals(msg.payload.toString(), payload.toString(),
                "xml payload should round-trip via TextMessage with the XML marker property set");
    }
}

@test:Config {groups: ["producer", "databinding"], dependsOn: [testProducerRoundTripXml]}
function testProducerRoundTripRecord() returns error? {
    ProducerBindingOrder payload = {id: "ORD-200", quantity: 3, expedited: true};
    check publishToQueue(BINDING_PRODUCER_QUEUE, payload);

    MessageConsumer consumer = check newBindingConsumer(BINDING_PRODUCER_QUEUE);
    record {|*Message; ProducerBindingOrder payload;|}? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is record {|*Message; ProducerBindingOrder payload;|}, "Should receive a message");
    if msg is record {|*Message; ProducerBindingOrder payload;|} {
        test:assertEquals(msg.payload, payload);
    }
}

@test:Config {groups: ["producer", "databinding"], dependsOn: [testProducerRoundTripRecord]}
function testProducerRoundTripScalars() returns error? {
    // int
    check publishToQueue(BINDING_PRODUCER_QUEUE, 123);
    MessageConsumer intConsumer = check newBindingConsumer(BINDING_PRODUCER_QUEUE);
    record {|*Message; int payload;|}? intMsg = check intConsumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check intConsumer->close();
    test:assertTrue(intMsg is record {|*Message; int payload;|}, "Should receive an int message");
    if intMsg is record {|*Message; int payload;|} {
        test:assertEquals(intMsg.payload, 123);
    }

    // float
    check publishToQueue(BINDING_PRODUCER_QUEUE, <float>9.5);
    MessageConsumer floatConsumer = check newBindingConsumer(BINDING_PRODUCER_QUEUE);
    record {|*Message; float payload;|}? floatMsg = check floatConsumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check floatConsumer->close();
    test:assertTrue(floatMsg is record {|*Message; float payload;|}, "Should receive a float message");
    if floatMsg is record {|*Message; float payload;|} {
        test:assertEquals(floatMsg.payload, <float>9.5);
    }

    // boolean
    check publishToQueue(BINDING_PRODUCER_QUEUE, true);
    MessageConsumer boolConsumer = check newBindingConsumer(BINDING_PRODUCER_QUEUE);
    record {|*Message; boolean payload;|}? boolMsg = check boolConsumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check boolConsumer->close();
    test:assertTrue(boolMsg is record {|*Message; boolean payload;|}, "Should receive a boolean message");
    if boolMsg is record {|*Message; boolean payload;|} {
        test:assertEquals(boolMsg.payload, true);
    }
}

@test:Config {groups: ["producer", "databinding"], dependsOn: [testProducerRoundTripScalars]}
function testProducerRoundTripNilPayload() returns error? {
    check publishToQueue(BINDING_PRODUCER_QUEUE, ());

    MessageConsumer consumer = check newBindingConsumer(BINDING_PRODUCER_QUEUE);
    BytesPayloadMessage? msg = check consumer->receive(DEFAULT_RECEIVE_TIMEOUT);
    check consumer->close();

    test:assertTrue(msg is BytesPayloadMessage, "Should receive a message");
    if msg is BytesPayloadMessage {
        test:assertEquals(msg.payload, [],
                "Nil payload should round-trip as an empty byte array, not the literal 'null' string");
    }
}
