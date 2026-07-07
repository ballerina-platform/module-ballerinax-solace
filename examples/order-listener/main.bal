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

import ballerina/io;
import ballerinax/solace;

// Broker connection settings (override via Config.toml, e.g. for Solace Cloud).
configurable string brokerUrl = "tcp://localhost:55554";
configurable string vpnName = "default";
configurable string username = "admin";
configurable string password = "admin";

// Whether to publish a few sample messages at startup so the example is self-contained.
configurable boolean publishSampleMessages = true;
configurable int sampleMessageCount = 3;

// The queue must be a compile-time constant because it is used in the @solace:ServiceConfig
// annotation. It must be provisioned on the broker (see README.md).
const string QUEUE_NAME = "demo/orders";

// Publish sample messages before the listener starts. They spool on the durable queue and are
// delivered to the service as soon as the listener starts.
function init() returns error? {
    if !publishSampleMessages {
        return;
    }
    solace:MessageProducer producer = check new (brokerUrl, {
        vpnName,
        auth: {username, password}
    });
    foreach int i in 1 ... sampleMessageCount {
        check producer->send({queueName: QUEUE_NAME}, {
            payload: string `Order-${i}`.toBytes(),
            deliveryMode: solace:PERSISTENT
        });
    }
    check producer->close();
    io:println(string `[init] Published ${sampleMessageCount} sample message(s) to queue '${QUEUE_NAME}'`);
    io:println(string `[init] Listening on queue '${QUEUE_NAME}' at ${brokerUrl}. Press Ctrl+C to stop.`);
}

// Asynchronous (push-based) consumption: the broker delivers each message to `onMessage`.
// Note: a listener program must not declare a `main` function; the Ballerina runtime keeps the
// program alive and manages the listener lifecycle automatically.
listener solace:Listener orderListener = check new (brokerUrl, {
    vpnName,
    auth: {username, password}
});

@solace:ServiceConfig {
    queueName: QUEUE_NAME,
    ackMode: solace:CLIENT_ACK
}
service on orderListener {

    remote function onMessage(solace:Message message, solace:Caller caller) returns error? {
        string payload = check string:fromBytes(message.payload);
        io:println(string `[onMessage] received: '${payload}'`);

        // Acknowledge only after successful processing (CLIENT_ACK mode).
        // On failure, skip the ack (or use caller->nack) and the broker redelivers the message.
        check caller->ack(message);
        io:println(string `[onMessage] acknowledged: '${payload}'`);
    }

    remote function onError(solace:Error err) returns error? {
        io:println(string `[onError] ${err.message()}`);
    }
}
