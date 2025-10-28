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

import ballerina/lang.runtime;
import ballerina/test;

// Test infrastructure
listener Listener serviceDataBindingListener = check new Listener(BROKER_URL, {
    messageVpn: MESSAGE_VPN,
    enableDynamicDurables: true,
    auth: {
        username: BROKER_USERNAME,
        password: BROKER_PASSWORD
    }
});

final MessageProducer serviceDataBindingProducer = check new (BROKER_URL, {
    messageVpn: MESSAGE_VPN,
    enableDynamicDurables: true,
    auth: {
        username: BROKER_USERNAME,
        password: BROKER_PASSWORD
    },
    destination: {queueName: "service-databinding-queue"}
});

// Test data storage
isolated string receivedStringPayload = "";
isolated xml receivedXmlPayload = xml ``;
isolated json receivedJsonPayload = {};
isolated record {string name; int age; string sex;} receivedRecordPayload = {name: "", age: 0, sex: ""};
isolated map<Value> receivedMapPayload = {};
isolated int receivedIntPayload = 0;
isolated float receivedFloatPayload = 0.0;
isolated decimal receivedDecimalPayload = 0.0d;
isolated boolean receivedBooleanPayload = false;
isolated byte[] receivedByteArrayPayload = [];

// Error tracking
type ErrorState record {|
    boolean hasError;
    string errorMsg;
|};

isolated ErrorState serviceDataBindingErrorState = {hasError: false, errorMsg: ""};

@test:Config {groups: ["serviceDataBinding", "servicePrimitives"]}
isolated function testServiceStringDatabinding() returns error? {
    string testPayload = "Service test message";

    Service stringService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; string payload;|} message) returns error? {
            lock {
                receivedStringPayload = message.payload;
            }
        }
    };

    check serviceDataBindingListener.attach(stringService, "string-databinding-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(receivedStringPayload, testPayload, "String payload not received correctly by service");
    }
    check serviceDataBindingListener.detach(stringService);
}

@test:Config {groups: ["serviceDataBinding", "servicePrimitives"], dependsOn: [testServiceStringDatabinding]}
isolated function testServiceIntDatabinding() returns error? {
    int testPayload = 42;

    Service intService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; int payload;|} message) returns error? {
            lock {
                receivedIntPayload = message.payload;
            }
        }
    };

    check serviceDataBindingListener.attach(intService, "int-databinding-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(receivedIntPayload, testPayload, "Int payload not received correctly by service");
    }
    check serviceDataBindingListener.detach(intService);
}

@test:Config {groups: ["serviceDataBinding", "servicePrimitives"], dependsOn: [testServiceIntDatabinding]}
isolated function testServiceFloatDatabinding() returns error? {
    float testPayload = 3.14159;

    Service floatService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; float payload;|} message) returns error? {
            lock {
                receivedFloatPayload = message.payload;
            }
        }
    };

    check serviceDataBindingListener.attach(floatService, "float-databinding-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(receivedFloatPayload, testPayload, "Float payload not received correctly by service");
    }
    check serviceDataBindingListener.detach(floatService);
}

@test:Config {groups: ["serviceDataBinding", "servicePrimitives"], dependsOn: [testServiceFloatDatabinding]}
isolated function testServiceDecimalDatabinding() returns error? {
    decimal testPayload = 99.99d;

    Service decimalService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; decimal payload;|} message) returns error? {
            lock {
                receivedDecimalPayload = message.payload;
            }
        }
    };

    check serviceDataBindingListener.attach(decimalService, "decimal-databinding-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(receivedDecimalPayload, testPayload, "Decimal payload not received correctly by service");
    }
    check serviceDataBindingListener.detach(decimalService);
}

@test:Config {groups: ["serviceDataBinding", "servicePrimitives"], dependsOn: [testServiceDecimalDatabinding]}
isolated function testServiceBooleanDatabinding() returns error? {
    boolean testPayload = true;

    Service booleanService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; boolean payload;|} message) returns error? {
            lock {
                receivedBooleanPayload = message.payload;
            }
        }
    };

    check serviceDataBindingListener.attach(booleanService, "boolean-databinding-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(receivedBooleanPayload, testPayload, "Boolean payload not received correctly by service");
    }
    check serviceDataBindingListener.detach(booleanService);
}

@test:Config {groups: ["serviceDataBinding", "servicePrimitives"], dependsOn: [testServiceBooleanDatabinding]}
isolated function testServiceByteArrayDatabinding() returns error? {
    byte[] testPayload = [72, 101, 108, 108, 111]; // "Hello"

    Service byteArrayService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; byte[] payload;|} message) returns error? {
            lock {
                receivedByteArrayPayload = message.payload.cloneReadOnly();
            }
        }
    };

    check serviceDataBindingListener.attach(byteArrayService, "bytearray-databinding-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(receivedByteArrayPayload, testPayload.cloneReadOnly(), "Byte array payload not received correctly by service");
    }
    check serviceDataBindingListener.detach(byteArrayService);
}

@test:Config {groups: ["serviceDataBinding", "serviceComplex"], dependsOn: [testServiceByteArrayDatabinding]}
isolated function testServiceXmlDatabinding() returns error? {
    xml testPayload = xml `<order><id>12345</id><item>Widget</item></order>`;

    Service xmlService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; xml payload;|} message) returns error? {
            lock {
                receivedXmlPayload = message.payload.cloneReadOnly();
            }
        }
    };

    check serviceDataBindingListener.attach(xmlService, "xml-databinding-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(receivedXmlPayload, testPayload.cloneReadOnly(), "XML payload not received correctly by service");
    }
    check serviceDataBindingListener.detach(xmlService);
}

@test:Config {groups: ["serviceDataBinding", "serviceComplex"], dependsOn: [testServiceXmlDatabinding]}
isolated function testServiceJsonDatabinding() returns error? {
    json testPayload = {name: "Alice", age: 30, city: "New York"};

    Service jsonService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; json payload;|} message) returns error? {
            lock {
                receivedJsonPayload = message.payload.cloneReadOnly();
            }
        }
    };

    check serviceDataBindingListener.attach(jsonService, "json-databinding-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(receivedJsonPayload, testPayload.cloneReadOnly(), "JSON payload not received correctly by service");
    }
    check serviceDataBindingListener.detach(jsonService);
}

@test:Config {groups: ["serviceDataBinding", "serviceComplex"], dependsOn: [testServiceJsonDatabinding]}
isolated function testServiceRecordDatabinding() returns error? {
    record {string name; int age; string sex;} testPayload = {
        name: "Bob",
        age: 25,
        sex: "Male"
    };

    Service recordService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|
                    *Message;
                    record {string name; int age; string sex;} payload;
                |} message) returns error? {
            lock {
                receivedRecordPayload = message.payload.cloneReadOnly();
            }
        }
    };

    check serviceDataBindingListener.attach(recordService, "record-databinding-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(receivedRecordPayload, testPayload.cloneReadOnly(), "Record payload not received correctly by service");
    }
    check serviceDataBindingListener.detach(recordService);
}

@test:Config {groups: ["serviceDataBinding", "serviceComplex"], dependsOn: [testServiceRecordDatabinding]}
isolated function testServiceMapDatabinding() returns error? {
    map<Value> testPayload = {
        name: "Charlie",
        age: 35,
        active: true
    };

    Service mapService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; map<Value> payload;|} message) returns error? {
            lock {
                receivedMapPayload = message.payload.cloneReadOnly();
            }
        }
    };

    check serviceDataBindingListener.attach(mapService, "map-databinding-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(receivedMapPayload, testPayload.cloneReadOnly(), "Map payload not received correctly by service");
    }
    check serviceDataBindingListener.detach(mapService);
}

isolated string callerReceivedPayload = "";

@test:Config {groups: ["serviceDataBinding", "serviceCaller"], dependsOn: [testServiceMapDatabinding]}
isolated function testServiceDataBindingWithCaller() returns error? {
    string testPayload = "Message with caller";

    Service callerService = @ServiceConfig {
        queueName: "service-databinding-queue",
        sessionAckMode: CLIENT_ACKNOWLEDGE,
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; string payload;|} message, Caller caller) returns error? {
            lock {
                callerReceivedPayload = message.payload;
            }
            check caller->acknowledge(message);
        }
    };

    check serviceDataBindingListener.attach(callerService, "caller-databinding-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(callerReceivedPayload, testPayload, "Payload with caller not received correctly");
    }
    check serviceDataBindingListener.detach(callerService);
}

isolated record {
    string orderId;
    record {
        string productName;
        int quantity;
    } product;
} nestedRecordReceived = {orderId: "", product: {productName: "", quantity: 0}};

@test:Config {groups: ["serviceDataBinding", "serviceComplex"], dependsOn: [testServiceDataBindingWithCaller]}
isolated function testServiceNestedRecordDatabinding() returns error? {
    record {
        string orderId;
        record {
            string productName;
            int quantity;
        } product;
    } testPayload = {
        orderId: "ORD-999",
        product: {
            productName: "Laptop",
            quantity: 3
        }
    };

    Service nestedRecordService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|
                    *Message;
                    record {
                        string orderId;
                        record {
                            string productName;
                            int quantity;
                        } product;
                    } payload;
                |} message) returns error? {
            lock {
                nestedRecordReceived = message.payload.cloneReadOnly();
            }
        }
    };

    check serviceDataBindingListener.attach(nestedRecordService, "nested-record-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(nestedRecordReceived, testPayload.cloneReadOnly(), "Nested record not received correctly");
    }
    check serviceDataBindingListener.detach(nestedRecordService);
}

@test:Config {groups: ["serviceDataBinding", "serviceNegative"], dependsOn: [testServiceNestedRecordDatabinding]}
isolated function testServiceTypeMismatchIntToString() returns error? {
    int testPayload = 123;

    Service mismatchService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; string payload;|} message) returns error? {
            lock {
                receivedStringPayload = message.payload;
            }
        }

        remote function onError(Error err) {
            lock {
                serviceDataBindingErrorState = {hasError: true, errorMsg: err.message()};
            }
        }
    };

    check serviceDataBindingListener.attach(mismatchService, "mismatch-int-string-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertTrue(serviceDataBindingErrorState.hasError, "Service should have received a data binding error");
        test:assertTrue(serviceDataBindingErrorState.errorMsg.length() > 0, "Error message should not be empty");
        serviceDataBindingErrorState = {hasError: false, errorMsg: ""};
    }
    check serviceDataBindingListener.detach(mismatchService);
}

@test:Config {groups: ["serviceDataBinding", "serviceNegative"], dependsOn: [testServiceTypeMismatchIntToString]}
isolated function testServiceTypeMismatchStringToInt() returns error? {
    string testPayload = "not a number";

    Service mismatchService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; int payload;|} message) returns error? {
            lock {
                receivedIntPayload = message.payload;
            }
        }

        remote function onError(Error err) {
            lock {
                serviceDataBindingErrorState = {hasError: true, errorMsg: err.message()};
            }
        }
    };

    check serviceDataBindingListener.attach(mismatchService, "mismatch-string-int-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertTrue(serviceDataBindingErrorState.hasError, "Service should have received a data binding error");
        test:assertTrue(serviceDataBindingErrorState.errorMsg.length() > 0, "Error message should not be empty");
        serviceDataBindingErrorState = {hasError: false, errorMsg: ""};
    }
    check serviceDataBindingListener.detach(mismatchService);
}

@test:Config {groups: ["serviceDataBinding", "serviceNegative"], dependsOn: [testServiceTypeMismatchStringToInt]}
isolated function testServiceInvalidRecordStructure() returns error? {
    record {string name; string email;} testPayload = {
        name: "Test User",
        email: "test@example.com"
    };

    Service invalidStructService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|
                    *Message;
                    record {string name; int age; string email;} payload;
                |} message) returns error? {
        }

        remote function onError(Error err) {
            lock {
                serviceDataBindingErrorState = {hasError: true, errorMsg: err.message()};
            }
        }
    };

    check serviceDataBindingListener.attach(invalidStructService, "invalid-struct-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertTrue(serviceDataBindingErrorState.hasError, "Service should have received a data binding error");
        test:assertTrue(serviceDataBindingErrorState.errorMsg.length() > 0, "Error message should not be empty");
        serviceDataBindingErrorState = {hasError: false, errorMsg: ""};
    }
    check serviceDataBindingListener.detach(invalidStructService);
}

// @test:Config {groups: ["serviceDataBinding", "serviceNegative", "failingTest"], dependsOn: [testServiceInvalidRecordStructure]}
// isolated function testServiceXmlToRecordMismatch() returns error? {
//     xml testPayload = xml `<person><name>John</name><age>30</age></person>`;

//     boolean serviceDataBindingError = false;
//     string serviceDataBindingErrorMsg = "";

//     Service xmlToRecordService = @ServiceConfig {
//         queueName: "service-databinding-queue",
//         pollingInterval: 1,
//         receiveTimeout: 1
//     } service object {
//         remote function onMessage(record {|
//                     *Message;
//                     record {string name; int age;} payload;
//                 |} message) returns error? {
//         }

//         remote function onError(Error err) {
//             serviceDataBindingError = true;
//             serviceDataBindingErrorMsg = err.message();
//         }
//     };

//     check serviceDataBindingListener.attach(xmlToRecordService, "xml-record-mismatch-service");
//     check serviceDataBindingProducer->send({payload: testPayload});
//     runtime:sleep(2);

//     test:assertTrue(serviceDataBindingError, "Service should have received a data binding error");
//     test:assertTrue(serviceDataBindingErrorMsg.length() > 0, "Error message should not be empty");
//     check serviceDataBindingListener.detach(xmlToRecordService);
// }

isolated string emptyStringReceived = "initial";

@test:Config {groups: ["serviceDataBinding", "serviceEdgeCases"], dependsOn: [testServiceInvalidRecordStructure]}
isolated function testServiceEmptyString() returns error? {
    string testPayload = "";

    Service emptyStringService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; string payload;|} message) returns error? {
            lock {
                emptyStringReceived = message.payload;
            }
        }
    };

    check serviceDataBindingListener.attach(emptyStringService, "empty-string-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(emptyStringReceived, testPayload, "Empty string not handled correctly");
    }
    check serviceDataBindingListener.detach(emptyStringService);
}

isolated json emptyJsonReceived = {initial: "value"};

@test:Config {groups: ["serviceDataBinding", "serviceEdgeCases"], dependsOn: [testServiceEmptyString]}
isolated function testServiceEmptyJson() returns error? {
    json testPayload = {};

    Service emptyJsonService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; json payload;|} message) returns error? {
            lock {
                emptyJsonReceived = message.payload.cloneReadOnly();
            }
        }
    };

    check serviceDataBindingListener.attach(emptyJsonService, "empty-json-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(emptyJsonReceived, testPayload.cloneReadOnly(), "Empty JSON not handled correctly");
    }
    check serviceDataBindingListener.detach(emptyJsonService);
}

isolated string unicodeStringReceived = "";

@test:Config {groups: ["serviceDataBinding", "serviceEdgeCases"], dependsOn: [testServiceEmptyJson]}
isolated function testServiceUnicodeString() returns error? {
    string testPayload = "Unicode: 你好 مرحبا 🚀";

    Service unicodeService = @ServiceConfig {
        queueName: "service-databinding-queue",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(record {|*Message; string payload;|} message) returns error? {
            lock {
                unicodeStringReceived = message.payload;
            }
        }
    };

    check serviceDataBindingListener.attach(unicodeService, "unicode-service");
    check serviceDataBindingProducer->send({payload: testPayload});
    runtime:sleep(2);

    lock {
        test:assertEquals(unicodeStringReceived, testPayload, "Unicode string not handled correctly");
    }
    check serviceDataBindingListener.detach(unicodeService);
}
