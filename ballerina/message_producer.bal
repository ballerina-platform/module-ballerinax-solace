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

import ballerina/jballerina.java;
import ballerina/constraint;

# Solace Message Producer to send messages to both queues and topics.
public isolated client class MessageProducer {

    public isolated function init(string url, *ProducerConfiguration config) returns Error? {
        ProducerConfiguration|constraint:Error validated = constraint:validate(config);
        if validated is constraint:Error {
            return error Error(
                string `Error occurred while validating the producer configurations: ${validated.message()}`, validated);
        }
        return self.externInit(url, config);
    }

    isolated function externInit(string url, ProducerConfiguration config) returns Error? = @java:Method {
        name: "init",
        'class: "io.ballerina.lib.solace.producer.Actions"
    } external;

    # Sends a message to the Solace broker.
    # ```ballerina
    # check producer->send(message);
    # ```
    #
    # + message - Message to be sent to the Solace broker
    # + return - A `solace:Error` if there is an error or else `()`
    isolated remote function send(Message message) returns Error? = @java:Method {
        name: "send",
        'class: "io.ballerina.lib.solace.producer.Actions"
    } external;

    # Closes the message producer.
    # ```ballerina
    # check producer->close();
    # ```
    # + return - A `solace:Error` if there is an error or else `()`
    isolated remote function close() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.producer.Actions"
    } external;    
}
