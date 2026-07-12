// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org).
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

# MessageProducer for synchronous (blocking) message publishing to Solace.
#
# Supports both guaranteed (PERSISTENT/NON_PERSISTENT) and direct (DIRECT) message delivery modes.
# Messages are published to specified destinations (topics or queues).
#
# Example usage:
# ```ballerina
# final smf:MessageProducer producer = check new (
#     host = "tcp://broker:55555",
#     auth = {username: "default"}
# );
#
# smf:Message message = {
#     payload: "Hello Solace".toBytes(),
#     deliveryMode: smf:PERSISTENT,
#     priority: 4
# };
#
# check producer->send(message, {topicName: "orders/created"});
# check producer->close();
# ```
public isolated client class MessageProducer {

    # Initialize a new MessageProducer with the given connection configuration.
    #
    # + url - The broker URL with format: [protocol:]host[:port]
    # + config - The producer connection configuration
    # + return - Error if initialization fails
    public isolated function init(string url, *ProducerConfiguration config) returns Error? {
        check validateConfigurations(config);
        return self.initProducer(url, config);
    }

    isolated function initProducer(string url, ProducerConfiguration config) returns Error? = @java:Method {
        'class: "io.xlibb.solace.producer.ProducerActions",
        name: "init"
    } external;

    # Send a message to the specified destination.
    #
    # + message - The message to send (payload and optional properties)
    # + destination - The destination to send to (topic or queue)
    # + return - Error if send fails
    isolated remote function send(Message message, Destination destination) returns Error? {
        check validateMessage(message);
        string|map<Value>|byte[] payload = convertPayload(message.payload);
        map<Property> properties = prepareProperties(message);
        InternalMessage internalMessage = {
            payload,
            deliveryMode: message.deliveryMode,
            priority: message.priority,
            timeToLive: message.timeToLive,
            applicationMessageId: message.applicationMessageId,
            applicationMessageType: message.applicationMessageType,
            correlationId: message.correlationId,
            replyTo: message.replyTo,
            senderId: message.senderId,
            senderTimestamp: message.senderTimestamp,
            receiveTimestamp: message.receiveTimestamp,
            sequenceNumber: message.sequenceNumber,
            redelivered: message.redelivered,
            deliveryCount: message.deliveryCount,
            properties,
            userData: message.userData
        };
        return self.externSend(internalMessage, destination);
    }

    isolated function externSend(InternalMessage message, Destination destination) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.producer.ProducerActions",
        name: "send"
    } external;

    # Commit the current transaction.
    #
    # Only applicable in transacted mode. Commits all message operations since the last commit/rollback.
    #
    # + return - Error if commit fails
    isolated remote function 'commit() returns Error? = @java:Method {
        'class: "io.xlibb.solace.producer.ProducerActions",
        name: "commit"
    } external;

    # Rollback the current transaction.
    #
    # Only applicable in transacted mode. Rolls back all message operations since the last commit/rollback.
    #
    # + return - Error if rollback fails
    isolated remote function 'rollback() returns Error? = @java:Method {
        'class: "io.xlibb.solace.producer.ProducerActions",
        name: "rollback"
    } external;

    # Close the producer and release all resources.
    #
    # After calling this method, the producer cannot be used.
    #
    # + return - Error if close fails
    isolated remote function close() returns Error? = @java:Method {
        'class: "io.xlibb.solace.producer.ProducerActions",
        name: "close"
    } external;
}

// Narrows an arbitrary `anydata` payload down to the concrete wire shapes native code understands.
isolated function convertPayload(anydata payload) returns string|map<Value>|byte[] {
    if payload is string {
        return payload;
    } else if payload is map<Value> {
        return payload;
    } else if payload is byte[] {
        return payload;
    } else if payload is xml {
        return payload.toString();
    } else if payload is int|boolean|float|decimal {
        return payload.toString().toBytes();
    } else {
        return payload.toJsonString().toBytes();
    }
}

isolated function prepareProperties(Message message) returns map<Property> {
    map<Property> properties = {};
    if message.properties is map<Property> {
        properties = (<map<Property>>message.properties).clone();
    }
    if message.payload is xml {
        properties[SOLACE_ISXML_PROP] = true;
    }
    return properties;
}

