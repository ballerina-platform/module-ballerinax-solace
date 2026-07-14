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

import ballerina/jballerina.java;

# Listener for asynchronous (push-based) message consumption from Solace.
#
# Uses the Ballerina listener pattern for event-driven message processing. Services are attached to
# the listener and receive messages through the `onMessage` remote method as soon as the broker
# delivers them.
#
# Each attached service declares its subscription via the `@solace:ServiceConfig` annotation.
# - Queue subscriptions: guaranteed delivery via a `FlowReceiver`.
# - Durable topic endpoint subscriptions: guaranteed delivery via a `FlowReceiver`.
# - Direct topic subscriptions: at-most-once delivery via an `XMLMessageConsumer`.
#
# When `transacted: true` is set on the connection configuration, a single `TransactedSession` is
# shared by every service attached to this listener. Calling `caller->commit()` or
# `caller->rollback()` from one service's `onMessage` commits or rolls back that shared transaction
# for all attached services, not just the one that made the call. `AUTO_ACK` is not supported on a
# transacted listener (settlement must go through `commit()`/`rollback()`); use `CLIENT_ACK` instead.
# If independent transactions per service are required, attach each transacted service to its own
# `Listener` instance.
#
# Example queue listener:
# ```ballerina
# listener solace:Listener solaceListener = check new (
#     url = "tcp://broker:55555",
#     auth = {username: "default"}
# );
#
# @solace:ServiceConfig {
#     queueName: "orders",
#     ackMode: solace:CLIENT_ACK
# }
# service on solaceListener {
#     remote function onMessage(solace:Message message, solace:Caller caller) returns error? {
#         // process the message
#         check caller->ack(message);
#     }
# }
# ```
public isolated class Listener {

    # Initialize a new listener with the given connection configuration.
    #
    # + url - The broker URL with format: [protocol:]host[:port]
    # + config - The connection configuration (auth, SSL/TLS, retry, etc.)
    # + return - Error if initialization fails
    public isolated function init(string url, *ListenerConfiguration config) returns Error? {
        check validateConfigurations(config);
        return self.initListener(url, config);
    }

    isolated function initListener(string url, ListenerConfiguration config) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.listener.ListenerActions",
        name: "init"
    } external;

    # Attach a service to the listener.
    #
    # The service must declare a remote `onMessage` method and may optionally declare an `onError`
    # method. Its subscription is read from the `@solace:ServiceConfig` annotation.
    #
    # + s - The service object to attach
    # + name - Optional service name (ignored; subscription is taken from the annotation)
    # + return - Error if attachment fails
    public isolated function attach(Service s, string[]|string? name = ()) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.listener.ListenerActions"
    } external;

    # Detach a service from the listener.
    #
    # Stops and closes the flow/consumer associated with the service.
    #
    # + s - The service object to detach
    # + return - Error if detachment fails
    public isolated function detach(Service s) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.listener.ListenerActions"
    } external;

    # Start the listener.
    #
    # Begins delivering messages to all attached services.
    #
    # + return - Error if start fails
    public isolated function 'start() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.listener.ListenerActions"
    } external;

    # Gracefully stop the listener.
    #
    # Stops delivery to attached services and waits for in-flight message processing to complete
    # before closing resources.
    #
    # + return - Error if stop fails
    public isolated function gracefulStop() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.listener.ListenerActions"
    } external;

    # Immediately stop the listener.
    #
    # Stops delivery and closes resources without waiting for in-flight processing.
    #
    # + return - Error if stop fails
    public isolated function immediateStop() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.listener.ListenerActions"
    } external;
}
