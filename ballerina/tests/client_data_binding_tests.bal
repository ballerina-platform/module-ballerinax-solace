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

@test:Config {groups: ["dataBinding", "primitives"], dependsOn: [testMapDatabinding]}
isolated function testIntDatabinding() returns error? {
    int payload = 42;
    check databindingProducer->send({payload});

    record {|*Message; int payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed int payloads differ");
}

@test:Config {groups: ["dataBinding", "primitives"], dependsOn: [testIntDatabinding]}
isolated function testFloatDatabinding() returns error? {
    float payload = 3.14159;
    check databindingProducer->send({payload});

    record {|*Message; float payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed float payloads differ");
}

@test:Config {groups: ["dataBinding", "primitives"], dependsOn: [testFloatDatabinding]}
isolated function testDecimalDatabinding() returns error? {
    decimal payload = 99.99d;
    check databindingProducer->send({payload});

    record {|*Message; decimal payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed decimal payloads differ");
}

@test:Config {groups: ["dataBinding", "primitives"], dependsOn: [testDecimalDatabinding]}
isolated function testBooleanDatabinding() returns error? {
    boolean payload = true;
    check databindingProducer->send({payload});

    record {|*Message; boolean payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed boolean payloads differ");
}

@test:Config {groups: ["dataBinding", "primitives"], dependsOn: [testBooleanDatabinding]}
isolated function testByteArrayDatabinding() returns error? {
    byte[] payload = [72, 101, 108, 108, 111]; // "Hello" in bytes
    check databindingProducer->send({payload});

    record {|*Message; byte[] payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed byte array payloads differ");
}

@test:Config {groups: ["dataBinding", "complex"], dependsOn: [testByteArrayDatabinding]}
isolated function testNestedRecordDatabinding() returns error? {
    record {
        string orderId;
        record {
            string productName;
            int quantity;
            decimal price;
        } product;
        record {
            string street;
            string city;
            string zipCode;
        } shippingAddress;
    } payload = {
        orderId: "ORD-12345",
        product: {
            productName: "Laptop",
            quantity: 2,
            price: 1299.99d
        },
        shippingAddress: {
            street: "123 Main St",
            city: "San Francisco",
            zipCode: "94102"
        }
    };
    check databindingProducer->send({payload});

    record {|
        *Message;
        record {
            string orderId;
            record {
                string productName;
                int quantity;
                decimal price;
            } product;
            record {
                string street;
                string city;
                string zipCode;
            } shippingAddress;
        } payload;
    |}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed nested record payloads differ");
}

@test:Config {groups: ["dataBinding", "complex"], dependsOn: [testNestedRecordDatabinding]}
isolated function testJsonArrayDatabinding() returns error? {
    json payload = [
        {name: "Alice", age: 30},
        {name: "Bob", age: 25},
        {name: "Charlie", age: 35}
    ];
    check databindingProducer->send({payload});

    record {|*Message; json payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed JSON array payloads differ");
}

@test:Config {groups: ["dataBinding", "complex"], dependsOn: [testJsonArrayDatabinding]}
isolated function testRecordWithOptionalFields() returns error? {
    record {
        string name;
        int age;
        string? email;
        string? phone;
    } payload = {
        name: "Jane Doe",
        age: 28,
        email: "jane@example.com",
        phone: ()
    };
    check databindingProducer->send({payload});

    record {|
        *Message;
        record {
            string name;
            int age;
            string? email;
            string? phone;
        } payload;
    |}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed payloads with optional fields differ");
}

@test:Config {groups: ["dataBinding", "complex"], dependsOn: [testRecordWithOptionalFields]}
isolated function testRecordWithArrayFields() returns error? {
    record {
        string department;
        string[] employees;
        int[] employeeIds;
    } payload = {
        department: "Engineering",
        employees: ["Alice", "Bob", "Charlie"],
        employeeIds: [101, 102, 103]
    };
    check databindingProducer->send({payload});

    record {|
        *Message;
        record {
            string department;
            string[] employees;
            int[] employeeIds;
        } payload;
    |}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed payloads with array fields differ");
}

@test:Config {groups: ["dataBinding", "edgeCases"], dependsOn: [testRecordWithArrayFields]}
isolated function testEmptyStringDatabinding() returns error? {
    string payload = "";
    check databindingProducer->send({payload});

    record {|*Message; string payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed empty string payloads differ");
}

@test:Config {groups: ["dataBinding", "edgeCases"], dependsOn: [testEmptyStringDatabinding]}
isolated function testStringWithSpecialCharacters() returns error? {
    string payload = "Test with special chars: \n\t\"quotes\" 'apostrophes' & <xml> {json} @#$%^&*()";
    check databindingProducer->send({payload});

    record {|*Message; string payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed special character payloads differ");
}

@test:Config {groups: ["dataBinding", "edgeCases"], dependsOn: [testStringWithSpecialCharacters]}
isolated function testLargeStringPayload() returns error? {
    string baseString = "This is a test message. ";
    string payload = "";
    int i = 0;
    while i < 43690 {
        payload += baseString;
        i += 1;
    }
    check databindingProducer->send({payload});

    record {|*Message; string payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    // test:assertEquals(receivedMessage?.payload?.length(), payload.length(), "Large payload size differs");
}

@test:Config {groups: ["dataBinding", "edgeCases"], dependsOn: [testLargeStringPayload]}
isolated function testEmptyByteArray() returns error? {
    byte[] payload = [];
    check databindingProducer->send({payload});

    record {|*Message; byte[] payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed empty byte array payloads differ");
}

@test:Config {groups: ["dataBinding", "edgeCases"], dependsOn: [testEmptyByteArray]}
isolated function testEmptyMapDatabinding() returns error? {
    map<Value> payload = {};
    check databindingProducer->send({payload});

    record {|*Message; map<Value> payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed empty map payloads differ");
}

@test:Config {groups: ["dataBinding", "edgeCases"], dependsOn: [testEmptyMapDatabinding]}
isolated function testEmptyJsonObject() returns error? {
    json payload = {};
    check databindingProducer->send({payload});

    record {|*Message; json payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed empty JSON payloads differ");
}

@test:Config {groups: ["dataBinding", "edgeCases"], dependsOn: [testEmptyJsonObject]}
isolated function testEmptyJsonArray() returns error? {
    json payload = [];
    check databindingProducer->send({payload});

    record {|*Message; json payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed empty JSON array payloads differ");
}

@test:Config {groups: ["dataBinding", "edgeCases"], dependsOn: [testEmptyJsonArray]}
isolated function testZeroValues() returns error? {
    record {
        int zeroInt;
        float zeroFloat;
        decimal zeroDecimal;
    } payload = {
        zeroInt: 0,
        zeroFloat: 0.0,
        zeroDecimal: 0.0d
    };
    check databindingProducer->send({payload});

    record {|
        *Message;
        record {
            int zeroInt;
            float zeroFloat;
            decimal zeroDecimal;
        } payload;
    |}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed zero value payloads differ");
}

@test:Config {groups: ["dataBinding", "edgeCases"], dependsOn: [testZeroValues]}
isolated function testNegativeNumbers() returns error? {
    record {
        int negativeInt;
        float negativeFloat;
        decimal negativeDecimal;
    } payload = {
        negativeInt: -999,
        negativeFloat: -123.456,
        negativeDecimal: -9999.99d
    };
    check databindingProducer->send({payload});

    record {|
        *Message;
        record {
            int negativeInt;
            float negativeFloat;
            decimal negativeDecimal;
        } payload;
    |}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed negative number payloads differ");
}

@test:Config {groups: ["dataBinding", "edgeCases"], dependsOn: [testNegativeNumbers]}
isolated function testUnicodeCharacters() returns error? {
    string payload = "Unicode test: 你好 مرحبا हैलो こんにちは 🚀 🎉 ❤️";
    check databindingProducer->send({payload});

    record {|*Message; string payload;|}? receivedMessage = check databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage !is (), "Should receive a message");
    test:assertEquals(receivedMessage?.payload, payload, "Published and consumed unicode payloads differ");
}

@test:Config {groups: ["dataBinding", "negative"], dependsOn: [testUnicodeCharacters]}
isolated function testTypeMismatchIntToString() returns error? {
    int payload = 123;
    check databindingProducer->send({payload});

    record {|*Message; string payload;|}|error? receivedMessage = databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage is error, "Should return an error for type mismatch");
    if receivedMessage is error {
        test:assertEquals(receivedMessage.message(), 
        "Data binding failed: Cannot bind BytesMessage to type 'string'. Use TextMessage for string/xml payloads");
    }
}

@test:Config {groups: ["dataBinding", "negative"], dependsOn: [testTypeMismatchIntToString]}
isolated function testTypeMismatchStringToInt() returns error? {
    string payload = "not a number";
    check databindingProducer->send({payload});

    record {|*Message; int payload;|}|error? receivedMessage = databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage is error, "Should return an error for type mismatch");
    if receivedMessage is error {
        test:assertEquals(receivedMessage.message(), 
        "Data binding failed: Cannot bind TextMessage to type 'int'. Expected 'string' or 'xml'");
    }
}

@test:Config {groups: ["dataBinding", "negative", "testTypeCast"], dependsOn: [testTypeMismatchStringToInt]}
isolated function testTypeMismatchJsonToRecord() returns error? {
    json payload = {
        name: "Test",
        age: "not an int",
        extra: "field"
    };
    check databindingProducer->send({payload});

    record {|
        *Message;
        record {
            string name;
            int age;
        } payload;
    |}|error? receivedMessage = databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage is error, "Should return an error for incompatible record structure");
}

@test:Config {groups: ["dataBinding", "negative"], dependsOn: [testTypeMismatchJsonToRecord]}
isolated function testMissingRequiredFields() returns error? {
    record {
        string name;
    } payload = {
        name: "Incomplete"
    };
    check databindingProducer->send({payload});

    record {|
        *Message;
        record {
            string name;
            int age;
            string email;
        } payload;
    |}|error? receivedMessage = databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage is error, "Should return an error for missing required fields");
}

@test:Config {groups: ["dataBinding", "negative"], dependsOn: [testMissingRequiredFields]}
isolated function testInvalidXmlToRecord() returns error? {
    xml payload = xml `<person><name>John</name><age>30</age></person>`;
    check databindingProducer->send({payload});

    record {|
        *Message;
        record {
            string name;
            int age;
        } payload;
    |}|error? receivedMessage = databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage is error, "Should return an error when trying to bind XML to record");
    if receivedMessage is error {
        test:assertEquals(receivedMessage.message(), "Data binding failed: Cannot bind TextMessage to type 'solace:record {| string name; int age; anydata...; |}'. Expected 'string' or 'xml'");
    }
}

@test:Config {groups: ["dataBinding", "negative"], dependsOn: [testInvalidXmlToRecord]}
isolated function testMapToStrictRecord() returns error? {
    map<Value> payload = {
        name: "Test",
        age: 30,
        extraField: "unexpected"
    };
    check databindingProducer->send({payload});

    record {|
        *Message;
        record {|
            string name;
            int age;
        |} payload;
    |}|error? receivedMessage = databindingConsumer->receive(5.0);
    test:assertTrue(receivedMessage is error, "Should return an error for extra fields in closed record");
}
