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

import ballerina/constraint;
import ballerina/jballerina.java;

# Represents a Solace listener endpoint that can be used to receive messages from a Solace topic or a queue.
public isolated class Listener {

    # Initializes a new `solace:Listener`.
    # ```ballerina
    # listener solace:Listener messageListener = check new (
    #     brokerUrl = "smf://localhost:55554",
    #     messageVpn = "default",
    #     auth = {
    #         username: "admin",
    #         password: "admin"
    #     }
    # );
    # ```
    #
    # + config - configurations used when initializing the listener
    # + return - `solace:Error` if an error occurs or `()` otherwise
    public isolated function init(string url, *ListenerConfiguration config) returns Error? {
        ListenerConfiguration|constraint:Error validated = constraint:validate(config);
        if validated is constraint:Error {
            return error Error(
                string `Error occurred while validating the listener configurations: ${validated.message()}`, validated);
        }
        return self.initListener(url, validated);
    }

    isolated function initListener(string url, ListenerConfiguration config) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.listener.Listener",
        name: "init"
    } external;

    # Attaches a Solace service to the listener.
    # ```ballerina
    # check messageListener.attach(solaceSvc);
    # ```
    #
    # + 'service - service instance
    # + name - service name
    # + return - `solace:Error` if an error occurs or `()` otherwise
    public isolated function attach(Service 'service, string[]|string? name = ()) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.listener.Listener"
    } external;

    # Detaches a Solace service from the listener.
    # ```ballerina
    # check messageListener.detach(solaceSvc);
    # ```
    #
    # + 'service - service instance
    # + return - `solace:Error` if an error occurs or `()` otherwise
    public isolated function detach(Service 'service) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.listener.Listener"
    } external;

    # Starts the listener.
    # ```ballerina
    # check messageListener.'start();
    # ```
    #
    # + return - `solace:Error` if an error occurs or `()` otherwise
    public isolated function 'start() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.listener.Listener"
    } external;

    # Gracefully stops the listener.
    # ```ballerina
    # check messageListener.gracefulStop();
    # ```
    #
    # + return - `solace:Error` if an error occurs or `()` otherwise
    public isolated function gracefulStop() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.listener.Listener"
    } external;

    # Immediately stops the listener.
    # ```ballerina
    # check messageListener.immediateStop();
    # ```
    #
    # + return - `solace:Error` if an error occurs or `()` otherwise
    public isolated function immediateStop() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.listener.Listener"
    } external;
}
