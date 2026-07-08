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

import ballerina/lang.runtime;
import ballerina/test;

// ========================================
// Record types used by the data-binding matrix below
// ========================================
type DataBindingAddress record {|
    string city;
    string country;
|};

type DataBindingPerson record {|
    string name;
    int age;
    DataBindingAddress address;
|};

type DataBindingFlatRecord record {|
    string name;
    int age;
|};

// A record shape deliberately incompatible with `DataBindingFlatRecord` (extra required field), used
// to exercise the structural-conversion-failure error path.
type DataBindingIncompatibleRecord record {|
    string name;
    int age;
    string requiredExtraField;
|};

// ========================================
// Shared recorder for captured payloads/errors
// ========================================
isolated class BindingRecorder {
    private anydata[] payloads = [];
    private string[] errors = [];

    isolated function addPayload(anydata payload) {
        lock {
            self.payloads.push(payload.clone());
        }
    }

    isolated function addError(string message) {
        lock {
            self.errors.push(message);
        }
    }

    isolated function payloadCount() returns int {
        lock {
            return self.payloads.length();
        }
    }

    isolated function errorCount() returns int {
        lock {
            return self.errors.length();
        }
    }

    isolated function lastPayload() returns anydata {
        lock {
            return self.payloads.length() > 0 ? self.payloads[self.payloads.length() - 1].clone() : ();
        }
    }

    isolated function lastError() returns string {
        lock {
            return self.errors.length() > 0 ? self.errors[self.errors.length() - 1] : "";
        }
    }
}

isolated function waitForBindingPayload(BindingRecorder recorder) {
    int step = 0;
    while step < POLL_MAX_STEPS && recorder.payloadCount() < 1 {
        runtime:sleep(POLL_STEP);
        step += 1;
    }
}

isolated function waitForBindingError(BindingRecorder recorder) {
    int step = 0;
    while step < POLL_MAX_STEPS && recorder.errorCount() < 1 {
        runtime:sleep(POLL_STEP);
        step += 1;
    }
}

// ========================================
// Positive cases: all of the tests below run one at a time against the single shared
// `BINDING_SVC_POSITIVE_QUEUE` (each attaches its own service to its own listener, publishes one
// message, waits for it, and fully stops the listener before returning - so the queue is always empty
// when the next case's listener attaches). The `dependsOn` chain makes that ordering explicit rather
// than relying on default test-execution order.
// ========================================

// ----- string payload -----
final BindingRecorder stringBindingRecorder = new;

Service stringBindingService = @ServiceConfig {
    queueName: BINDING_SVC_POSITIVE_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; string payload;|} message) returns error? {
        stringBindingRecorder.addPayload(message.payload);
    }
};

@test:Config {groups: ["listener", "databinding"]}
function testServiceBindStringPayload() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(stringBindingService);
    check solaceListener.'start();
    runtime:sleep(2);

    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_POSITIVE_QUEUE}, {payload: "hello-string-binding"});
    check producer->close();

    waitForBindingPayload(stringBindingRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(stringBindingRecorder.payloadCount(), 1, "Should have received exactly one message");
    test:assertEquals(stringBindingRecorder.lastPayload(), "hello-string-binding");
}

// ----- int payload -----
final BindingRecorder intBindingRecorder = new;

Service intBindingService = @ServiceConfig {
    queueName: BINDING_SVC_POSITIVE_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; int payload;|} message) returns error? {
        intBindingRecorder.addPayload(message.payload);
    }
};

@test:Config {groups: ["listener", "databinding"], dependsOn: [testServiceBindStringPayload]}
function testServiceBindIntPayload() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(intBindingService);
    check solaceListener.'start();
    runtime:sleep(2);

    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_POSITIVE_QUEUE}, {payload: 42});
    check producer->close();

    waitForBindingPayload(intBindingRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(intBindingRecorder.payloadCount(), 1, "Should have received exactly one message");
    test:assertEquals(intBindingRecorder.lastPayload(), 42);
}

// ----- float payload -----
final BindingRecorder floatBindingRecorder = new;

Service floatBindingService = @ServiceConfig {
    queueName: BINDING_SVC_POSITIVE_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; float payload;|} message) returns error? {
        floatBindingRecorder.addPayload(message.payload);
    }
};

@test:Config {groups: ["listener", "databinding"], dependsOn: [testServiceBindIntPayload]}
function testServiceBindFloatPayload() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(floatBindingService);
    check solaceListener.'start();
    runtime:sleep(2);

    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_POSITIVE_QUEUE}, {payload: <float>3.5});
    check producer->close();

    waitForBindingPayload(floatBindingRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(floatBindingRecorder.payloadCount(), 1, "Should have received exactly one message");
    test:assertEquals(floatBindingRecorder.lastPayload(), <float>3.5);
}

// ----- decimal payload -----
final BindingRecorder decimalBindingRecorder = new;

Service decimalBindingService = @ServiceConfig {
    queueName: BINDING_SVC_POSITIVE_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; decimal payload;|} message) returns error? {
        decimalBindingRecorder.addPayload(message.payload);
    }
};

@test:Config {groups: ["listener", "databinding"], dependsOn: [testServiceBindFloatPayload]}
function testServiceBindDecimalPayload() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(decimalBindingService);
    check solaceListener.'start();
    runtime:sleep(2);

    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_POSITIVE_QUEUE}, {payload: 12.75d});
    check producer->close();

    waitForBindingPayload(decimalBindingRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(decimalBindingRecorder.payloadCount(), 1, "Should have received exactly one message");
    test:assertEquals(decimalBindingRecorder.lastPayload(), 12.75d);
}

// ----- boolean payload -----
final BindingRecorder booleanBindingRecorder = new;

Service booleanBindingService = @ServiceConfig {
    queueName: BINDING_SVC_POSITIVE_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; boolean payload;|} message) returns error? {
        booleanBindingRecorder.addPayload(message.payload);
    }
};

@test:Config {groups: ["listener", "databinding"], dependsOn: [testServiceBindDecimalPayload]}
function testServiceBindBooleanPayload() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(booleanBindingService);
    check solaceListener.'start();
    runtime:sleep(2);

    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_POSITIVE_QUEUE}, {payload: true});
    check producer->close();

    waitForBindingPayload(booleanBindingRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(booleanBindingRecorder.payloadCount(), 1, "Should have received exactly one message");
    test:assertEquals(booleanBindingRecorder.lastPayload(), true);
}

// ----- byte[] payload -----
final BindingRecorder bytesBindingRecorder = new;

Service bytesBindingService = @ServiceConfig {
    queueName: BINDING_SVC_POSITIVE_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; byte[] payload;|} message) returns error? {
        bytesBindingRecorder.addPayload(message.payload);
    }
};

@test:Config {groups: ["listener", "databinding"], dependsOn: [testServiceBindBooleanPayload]}
function testServiceBindBytesPayload() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(bytesBindingService);
    check solaceListener.'start();
    runtime:sleep(2);

    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_POSITIVE_QUEUE}, {payload: "hello-bytes-binding".toBytes()});
    check producer->close();

    waitForBindingPayload(bytesBindingRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(bytesBindingRecorder.payloadCount(), 1, "Should have received exactly one message");
    test:assertEquals(bytesBindingRecorder.lastPayload(), "hello-bytes-binding".toBytes());
}

// ----- xml payload -----
final BindingRecorder xmlBindingRecorder = new;

Service xmlBindingService = @ServiceConfig {
    queueName: BINDING_SVC_POSITIVE_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; xml payload;|} message) returns error? {
        xmlBindingRecorder.addPayload(message.payload.toString());
    }
};

@test:Config {groups: ["listener", "databinding"], dependsOn: [testServiceBindBytesPayload]}
function testServiceBindXmlPayload() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(xmlBindingService);
    check solaceListener.'start();
    runtime:sleep(2);

    xml orderXml = xml `<order><id>42</id></order>`;
    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_POSITIVE_QUEUE}, {payload: orderXml});
    check producer->close();

    waitForBindingPayload(xmlBindingRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(xmlBindingRecorder.payloadCount(), 1, "Should have received exactly one message");
    test:assertEquals(xmlBindingRecorder.lastPayload(), orderXml.toString());
}

// ----- json payload -----
// Deliberately includes an array value so the payload is NOT structurally a `map<Value>` (Value
// disallows arrays other than byte[]); this forces the producer's fallback `toJsonString().toBytes()`
// serialization (a BytesMessage) instead of the map<Value> path (a MapMessage).
final BindingRecorder jsonBindingRecorder = new;

Service jsonBindingService = @ServiceConfig {
    queueName: BINDING_SVC_POSITIVE_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; json payload;|} message) returns error? {
        jsonBindingRecorder.addPayload(message.payload);
    }
};

@test:Config {groups: ["listener", "databinding"], dependsOn: [testServiceBindXmlPayload]}
function testServiceBindJsonPayload() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(jsonBindingService);
    check solaceListener.'start();
    runtime:sleep(2);

    json orderJson = {name: "json-order", tags: ["a", "b"]};
    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_POSITIVE_QUEUE}, {payload: orderJson});
    check producer->close();

    waitForBindingPayload(jsonBindingRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(jsonBindingRecorder.payloadCount(), 1, "Should have received exactly one message");
    test:assertEquals(jsonBindingRecorder.lastPayload(), orderJson);
}

// ----- flat record payload -----
final BindingRecorder recordBindingRecorder = new;

Service recordBindingService = @ServiceConfig {
    queueName: BINDING_SVC_POSITIVE_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; DataBindingFlatRecord payload;|} message) returns error? {
        recordBindingRecorder.addPayload(message.payload);
    }
};

@test:Config {groups: ["listener", "databinding"], dependsOn: [testServiceBindJsonPayload]}
function testServiceBindFlatRecordPayload() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(recordBindingService);
    check solaceListener.'start();
    runtime:sleep(2);

    DataBindingFlatRecord flatRecord = {name: "Alice", age: 30};
    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_POSITIVE_QUEUE}, {payload: flatRecord});
    check producer->close();

    waitForBindingPayload(recordBindingRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(recordBindingRecorder.payloadCount(), 1, "Should have received exactly one message");
    test:assertEquals(recordBindingRecorder.lastPayload(), flatRecord);
}

// ----- nested record payload -----
final BindingRecorder nestedRecordBindingRecorder = new;

Service nestedRecordBindingService = @ServiceConfig {
    queueName: BINDING_SVC_POSITIVE_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; DataBindingPerson payload;|} message) returns error? {
        nestedRecordBindingRecorder.addPayload(message.payload);
    }
};

@test:Config {groups: ["listener", "databinding"], dependsOn: [testServiceBindFlatRecordPayload]}
function testServiceBindNestedRecordPayload() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(nestedRecordBindingService);
    check solaceListener.'start();
    runtime:sleep(2);

    DataBindingPerson person = {name: "Bob", age: 40, address: {city: "Colombo", country: "LK"}};
    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_POSITIVE_QUEUE}, {payload: person});
    check producer->close();

    waitForBindingPayload(nestedRecordBindingRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(nestedRecordBindingRecorder.payloadCount(), 1, "Should have received exactly one message");
    test:assertEquals(nestedRecordBindingRecorder.lastPayload(), person);
}

// ----- map<Value> payload (with a nested map entry) -----
final BindingRecorder mapBindingRecorder = new;

Service mapBindingService = @ServiceConfig {
    queueName: BINDING_SVC_POSITIVE_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; map<Value> payload;|} message) returns error? {
        mapBindingRecorder.addPayload(message.payload);
    }
};

@test:Config {groups: ["listener", "databinding"], dependsOn: [testServiceBindNestedRecordPayload]}
function testServiceBindMapPayload() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(mapBindingService);
    check solaceListener.'start();
    runtime:sleep(2);

    map<Value> orderMap = {id: 7, active: true, tags: {a: "b"}};
    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_POSITIVE_QUEUE}, {payload: orderMap});
    check producer->close();

    waitForBindingPayload(mapBindingRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(mapBindingRecorder.payloadCount(), 1, "Should have received exactly one message");
    test:assertEquals(mapBindingRecorder.lastPayload(), orderMap);
}

// ----- data binding combined with a Caller parameter -----
final BindingRecorder callerBindingRecorder = new;

Service callerBindingService = @ServiceConfig {
    queueName: BINDING_SVC_POSITIVE_QUEUE,
    ackMode: CLIENT_ACK
} service object {
    remote function onMessage(record {|*Message; string payload;|} message, Caller caller) returns error? {
        callerBindingRecorder.addPayload(message.payload);
        check caller->ack(message);
    }
};

@test:Config {groups: ["listener", "databinding"], dependsOn: [testServiceBindMapPayload]}
function testServiceBindWithCaller() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(callerBindingService);
    check solaceListener.'start();
    runtime:sleep(2);

    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_POSITIVE_QUEUE}, {payload: "hello-caller-binding"});
    check producer->close();

    waitForBindingPayload(callerBindingRecorder);
    // Give the acknowledgement a moment to reach the broker before checking the queue.
    runtime:sleep(1);
    check solaceListener.gracefulStop();

    boolean queueEmpty = check queueIsEmpty(BINDING_SVC_POSITIVE_QUEUE);
    test:assertEquals(callerBindingRecorder.payloadCount(), 1, "Should have received exactly one message");
    test:assertEquals(callerBindingRecorder.lastPayload(), "hello-caller-binding");
    test:assertTrue(queueEmpty, "caller->ack should have acknowledged the narrowed-record message");
}

// ========================================
// Negative cases: each keeps its own dedicated queue (NOT shared with anything else). A failed
// data-binding attempt deliberately leaves the message unacknowledged so guaranteed flows redeliver
// it (see SolaceMessageListener.deliver) - sharing a queue here would let one test's poisoned,
// redeliverable message leak into an unrelated later test.
// ========================================

// ----- int sent (-> BytesMessage), string expected -----
final BindingRecorder mismatchStringRecorder = new;

Service mismatchStringExpectedService = @ServiceConfig {
    queueName: BINDING_SVC_MISMATCH_STRING_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; string payload;|} message) returns error? {
    }
    remote function onError(Error err) returns error? {
        mismatchStringRecorder.addError(err.message());
    }
};

@test:Config {groups: ["listener", "databinding", "negative"]}
function testServiceBindMismatchIntSentStringExpected() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(mismatchStringExpectedService);
    check solaceListener.'start();
    runtime:sleep(2);

    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_MISMATCH_STRING_QUEUE}, {payload: 42});
    check producer->close();

    waitForBindingError(mismatchStringRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(mismatchStringRecorder.errorCount(), 1, "onError should have been invoked once");
    test:assertEquals(mismatchStringRecorder.lastError(),
            "Data binding failed: Cannot bind BytesMessage to type 'string'. Use TextMessage for string/xml payloads");
}

// ----- string sent (-> TextMessage), int expected -----
final BindingRecorder mismatchIntRecorder = new;

Service mismatchIntExpectedService = @ServiceConfig {
    queueName: BINDING_SVC_MISMATCH_INT_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; int payload;|} message) returns error? {
    }
    remote function onError(Error err) returns error? {
        mismatchIntRecorder.addError(err.message());
    }
};

@test:Config {groups: ["listener", "databinding", "negative"]}
function testServiceBindMismatchStringSentIntExpected() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(mismatchIntExpectedService);
    check solaceListener.'start();
    runtime:sleep(2);

    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_MISMATCH_INT_QUEUE}, {payload: "not-an-int"});
    check producer->close();

    waitForBindingError(mismatchIntRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(mismatchIntRecorder.errorCount(), 1, "onError should have been invoked once");
    test:assertEquals(mismatchIntRecorder.lastError(),
            "Data binding failed: Cannot bind TextMessage to type 'int'. Expected 'string' or 'xml'");
}

// ----- incompatible record shape -----
final BindingRecorder mismatchRecordRecorder = new;

Service mismatchRecordExpectedService = @ServiceConfig {
    queueName: BINDING_SVC_MISMATCH_RECORD_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; DataBindingIncompatibleRecord payload;|} message) returns error? {
    }
    remote function onError(Error err) returns error? {
        mismatchRecordRecorder.addError(err.message());
    }
};

@test:Config {groups: ["listener", "databinding", "negative"]}
function testServiceBindMismatchIncompatibleRecord() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(mismatchRecordExpectedService);
    check solaceListener.'start();
    runtime:sleep(2);

    DataBindingFlatRecord flatRecord = {name: "Charlie", age: 25};
    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    check producer->send({queueName: BINDING_SVC_MISMATCH_RECORD_QUEUE}, {payload: flatRecord});
    check producer->close();

    waitForBindingError(mismatchRecordRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(mismatchRecordRecorder.errorCount(), 1, "onError should have been invoked once");
    test:assertTrue(mismatchRecordRecorder.lastError().startsWith("Data binding failed: "),
            "Error message should indicate a data binding failure");
}

// ----- no onError declared: a failed bind must not wedge subsequent message processing -----
// Exercises SolaceMessageListener.dispatchError's "no handler" logging path (prints to stderr instead
// of silently dropping the error). This test cannot assert on the stderr content itself; it verifies
// the functionally important part - that the dispatcher recovers and keeps processing.
final BindingRecorder noOnErrorRecorder = new;

Service noOnErrorBindingService = @ServiceConfig {
    queueName: BINDING_SVC_NO_ONERROR_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(record {|*Message; string payload;|} message) returns error? {
        noOnErrorRecorder.addPayload(message.payload);
    }
};

@test:Config {groups: ["listener", "databinding", "negative"]}
function testServiceBindMismatchWithNoOnErrorHandler() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(noOnErrorBindingService);
    check solaceListener.'start();
    runtime:sleep(2);

    MessageProducer producer = check new (BROKER_URL, connectionConfig());
    // First message: wrong wire shape for 'string' -> binding fails; no onError exists to observe it.
    check producer->send({queueName: BINDING_SVC_NO_ONERROR_QUEUE}, {payload: 42});
    runtime:sleep(1);
    // Second message: well-formed -> must still be delivered, proving the dispatcher wasn't wedged.
    check producer->send({queueName: BINDING_SVC_NO_ONERROR_QUEUE}, {payload: "still-works"});
    check producer->close();

    waitForBindingPayload(noOnErrorRecorder);
    check solaceListener.gracefulStop();

    test:assertEquals(noOnErrorRecorder.payloadCount(), 1, "Only the well-formed message should be bound");
    test:assertEquals(noOnErrorRecorder.lastPayload(), "still-works");
}
