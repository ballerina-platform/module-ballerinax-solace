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

// ========================================
// Service-shape validation tests (attach()-time; no message ever flows, so a single shared
// throwaway queue is reused across all cases).
// ========================================

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationMissingServiceConfig() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    Service noAnnotationService = service object {
        remote function onMessage(Message message) returns error? {
        }
    };
    error? result = solaceListener.attach(noAnnotationService);
    test:assertTrue(result is error, "Attaching a service without @ServiceConfig should fail");
    if result is error {
        test:assertEquals(result.message(),
                "The @solace:ServiceConfig annotation with a queue or topic subscription is required");
    }
    check solaceListener.gracefulStop();
}

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationResourceMethodsRejected() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    Service resourceService = @ServiceConfig {
        queueName: BINDING_VALIDATION_QUEUE
    } service object {
        resource function get greeting() returns string => "hello";
        remote function onMessage(Message message) returns error? {
        }
    };
    error? result = solaceListener.attach(resourceService);
    test:assertTrue(result is error, "Attaching a service with resource methods should fail");
    if result is error {
        test:assertEquals(result.message(), "Solace service cannot have resource methods.");
    }
    check solaceListener.gracefulStop();
}

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationNoRemoteMethods() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    Service noMethodsService = @ServiceConfig {
        queueName: BINDING_VALIDATION_QUEUE
    } service object {
    };
    error? result = solaceListener.attach(noMethodsService);
    test:assertTrue(result is error, "A service with no remote methods should fail");
    if result is error {
        test:assertEquals(result.message(), "Solace service must have exactly one or two remote methods.");
    }
    check solaceListener.gracefulStop();
}

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationTooManyRemoteMethods() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    Service tooManyMethodsService = @ServiceConfig {
        queueName: BINDING_VALIDATION_QUEUE
    } service object {
        remote function onMessage(Message message) returns error? {
        }
        remote function onError(Error err) returns error? {
        }
        remote function onExtra(Message message) returns error? {
        }
    };
    error? result = solaceListener.attach(tooManyMethodsService);
    test:assertTrue(result is error, "Attaching a service with more than two remote methods should fail");
    if result is error {
        test:assertEquals(result.message(), "Solace service must have exactly one or two remote methods.");
    }
    check solaceListener.gracefulStop();
}

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationInvalidRemoteMethodName() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    Service invalidNameService = @ServiceConfig {
        queueName: BINDING_VALIDATION_QUEUE
    } service object {
        remote function onEvent(Message message) returns error? {
        }
    };
    error? result = solaceListener.attach(invalidNameService);
    test:assertTrue(result is error, "Attaching a service with an unrecognized remote method name should fail");
    if result is error {
        test:assertEquals(result.message(), "Invalid remote method name: onEvent.");
    }
    check solaceListener.gracefulStop();
}

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationMissingOnMessageMethod() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    Service onlyOnErrorService = @ServiceConfig {
        queueName: BINDING_VALIDATION_QUEUE
    } service object {
        remote function onError(Error err) returns error? {
        }
    };
    error? result = solaceListener.attach(onlyOnErrorService);
    test:assertTrue(result is error, "A service without an onMessage method should fail");
    if result is error {
        test:assertEquals(result.message(), "The service must declare a remote 'onMessage' method");
    }
    check solaceListener.gracefulStop();
}

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationOnMessageTooManyParams() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    Service tooManyParamsService = @ServiceConfig {
        queueName: BINDING_VALIDATION_QUEUE
    } service object {
        remote function onMessage(Message message, Caller caller, Message extra) returns error? {
        }
    };
    error? result = solaceListener.attach(tooManyParamsService);
    test:assertTrue(result is error, "onMessage with more than two parameters should fail");
    if result is error {
        test:assertEquals(result.message(), "onMessage method can have only have either one or two parameters.");
    }
    check solaceListener.gracefulStop();
}

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationOnMessageWrongParamType() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    Service wrongParamTypeService = @ServiceConfig {
        queueName: BINDING_VALIDATION_QUEUE
    } service object {
        remote function onMessage(string payload) returns error? {
        }
    };
    error? result = solaceListener.attach(wrongParamTypeService);
    test:assertTrue(result is error, "onMessage with a non-Message/Caller parameter should fail");
    if result is error {
        test:assertEquals(result.message(),
                "onMessage method parameters must be of type 'solace:Message' (or its subtype) or 'solace:Caller'.");
    }
    check solaceListener.gracefulStop();
}

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationOnMessageMissingMessageParam() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    Service callerOnlyService = @ServiceConfig {
        queueName: BINDING_VALIDATION_QUEUE
    } service object {
        remote function onMessage(Caller caller) returns error? {
        }
    };
    error? result = solaceListener.attach(callerOnlyService);
    test:assertTrue(result is error, "onMessage without a Message parameter should fail");
    if result is error {
        test:assertEquals(result.message(), "Required parameter 'solace:Message' can not be found.");
    }
    check solaceListener.gracefulStop();
}

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationOnErrorWrongParamCount() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    Service badOnErrorService = @ServiceConfig {
        queueName: BINDING_VALIDATION_QUEUE
    } service object {
        remote function onMessage(Message message) returns error? {
        }
        remote function onError() returns error? {
        }
    };
    error? result = solaceListener.attach(badOnErrorService);
    test:assertTrue(result is error, "onError with zero parameters should fail");
    if result is error {
        test:assertEquals(result.message(), "onError method must have exactly one parameter of type 'solace:Error'.");
    }
    check solaceListener.gracefulStop();
}

@test:Config {groups: ["listener", "validation", "negative"]}
function testValidationOnErrorWrongParamType() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    Service wrongOnErrorTypeService = @ServiceConfig {
        queueName: BINDING_VALIDATION_QUEUE
    } service object {
        remote function onMessage(Message message) returns error? {
        }
        remote function onError(Message err) returns error? {
        }
    };
    error? result = solaceListener.attach(wrongOnErrorTypeService);
    test:assertTrue(result is error, "onError with a non-Error parameter should fail");
    if result is error {
        test:assertEquals(result.message(), "onError method parameter must be of type 'solace:Error'.");
    }
    check solaceListener.gracefulStop();
}
