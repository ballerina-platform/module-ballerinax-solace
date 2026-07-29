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

import ballerinax/solace;

solace:Service missingAnnotation = service object {
    remote function onMessage(solace:Message message) returns error? {}
};

solace:Service resourceMethod = @solace:ServiceConfig {queueName: "q"} service object {
    resource function get status() returns string => "ok";
    remote function onMessage(solace:Message message) returns error? {}
};

solace:Service missingOnMessage = @solace:ServiceConfig {queueName: "q"} service object {
};

solace:Service unsupportedMethod = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onEvent(solace:Message message) returns error? {}
};

solace:Service invalidParameterCount = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage() returns error? {}
};

solace:Service invalidMessageType = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage(string payload) returns error? {}
};

solace:Service invalidCallerPosition = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage(solace:Message message, solace:Message second) returns error? {}
};

solace:Service invalidOnError = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage(solace:Message message) returns error? {}
    remote function onError(solace:Message err) returns error? {}
};

solace:Service invalidReturn = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage(solace:Message message) returns string => "invalid";
};

type StringMessage record {|
    *solace:Message;
    string payload;
|};

solace:Service validService = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage(StringMessage message, solace:Caller caller) returns solace:Error? {}
    remote function onError(solace:Error err) returns error? {}
};
