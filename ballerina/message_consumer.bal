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

# Solace Message Consumer to receive messages from both queues and topics.
public isolated client class MessageConsumer {

    # Initializes a new Solace message consumer with the given broker URL and configuration.
    # ```ballerina
    # solace:MessageConsumer consumer = check new (brokerUrl, {
    #     subscriptionConfig: {queueName: "orders"}
    # });
    # ```
    #
    # + url - The Solace broker URL in the format `<scheme>://[username]:[password]@<host>[:port]`.
    # Supported schemes are `smf` (plain-text) and `smfs` (TLS/SSL).
    # Multiple hosts can be specified as a comma-separated list for failover support.
    # Default ports: 55555 (standard), 55003 (compression), 55443 (SSL)
    # + config - Consumer configuration including connection settings and subscription details
    # + return - A `solace:Error` if initialization fails or else `()`
    public isolated function init(string url, *ConsumerConfiguration config) returns Error? {
        Error? validated = validateConfigurations(config);
        if validated is Error {
            return error Error(
                string `Error occurred while validating the consumer configurations: ${validated.message()}`, validated);
        }
        return self.externInit(url, config);
    }

    isolated function externInit(string url, ConsumerConfiguration config) returns Error? = @java:Method {
        name: "init",
        'class: "io.ballerina.lib.solace.consumer.Actions"
    } external;

    # Receives the next message from the Solace broker, waiting up to the specified timeout.
    # ```ballerina
    # solace:Message? message = check consumer->receive(5.0);
    # ```
    #
    # + timeout - The maximum time to wait for a message in seconds. Default is 10.0 seconds
    # + T - Optional type description of the expected data type
    # + return - The received `Message`, `()` if no message is available within the timeout, or a `solace:Error` if there is an error
    isolated remote function receive(decimal timeout = 10.0, typedesc<Message> T = <>) returns T|Error? = @java:Method {
        'class: "io.ballerina.lib.solace.consumer.Actions"
    } external;

    # Receives the next message from the Solace broker if one is immediately available, without waiting.
    # ```ballerina
    # solace:Message? message = check consumer->receiveNoWait();
    # ```
    # 
    # + T - Optional type description of the expected data type
    # + return - The received `Message` if immediately available, `()` if no message is available, or a `solace:Error` if there is an error
    isolated remote function receiveNoWait(typedesc<Message> T = <>) returns T|Error? = @java:Method {
        'class: "io.ballerina.lib.solace.consumer.Actions"
    } external;

    # Acknowledges the specified message. This method should only be called when the consumer is configured
    # with `sessionAckMode: CLIENT_ACKNOWLEDGE`.
    # ```ballerina
    # check consumer->acknowledge(message);
    # ```
    #
    # + message - The message to acknowledge
    # + return - A `solace:Error` if there is an error or else `()`
    isolated remote function acknowledge(Message message) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.consumer.Actions"
    } external;

    # Commits all messages received in this transaction and releases any locks currently held.
    # This method should only be called when the consumer is configured with `sessionAckMode: SESSION_TRANSACTED`.
    # ```ballerina
    # check consumer->'commit();
    # ```
    #
    # + return - A `solace:Error` if there is an error or else `()`
    isolated remote function 'commit() returns Error? = @java:Method {
        name: "commit",
        'class: "io.ballerina.lib.solace.consumer.Actions"
    } external;

    # Rolls back any messages received in this transaction and releases any locks currently held.
    # This method should only be called when the consumer is configured with `sessionAckMode: SESSION_TRANSACTED`.
    # ```ballerina
    # check consumer->'rollback();
    # ```
    #
    # + return - A `solace:Error` if there is an error or else `()`
    isolated remote function 'rollback() returns Error? = @java:Method {
        name: "rollback",
        'class: "io.ballerina.lib.solace.consumer.Actions"
    } external;

    # Closes the message consumer and releases all resources.
    # ```ballerina
    # check consumer->close();
    # ```
    #
    # + return - A `solace:Error` if there is an error or else `()`
    isolated remote function close() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.consumer.Actions"
    } external;
}
