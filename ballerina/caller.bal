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

# Caller for explicit acknowledgement and transaction control within a service's `onMessage` method.
#
# A `Caller` is created by the `solace:Listener` and supplied as the optional second parameter of
# `onMessage`. Use it when the service is configured with `ackMode = CLIENT_ACK` and needs manual
# acknowledgement, or when the listener connection is transacted and needs commit/rollback control.
public isolated client class Caller {

    # Acknowledge a message in `CLIENT_ACK` mode.
    #
    # In `AUTO_ACK` mode messages are acknowledged automatically after `onMessage` returns
    # successfully and this method should not be called. When the listener connection is transacted,
    # settlement is governed by `commit`/`rollback`, so this method does not apply.
    #
    # + message - The message to acknowledge
    # + return - Error if acknowledgement fails
    isolated remote function ack(Message message) returns Error? = @java:Method {
        'class: "io.xlibb.solace.caller.CallerActions"
    } external;

    # Negatively acknowledge a message (NACK).
    #
    # When the listener connection is transacted, settlement is governed by `commit`/`rollback`, so
    # this method does not apply.
    #
    # + message - The message to negatively acknowledge
    # + requeue - If true, the message is requeued for redelivery (FAILED outcome).
    # If false, the message moves to the DMQ immediately, if configured. If not, the message is simply
    # discarded. (REJECTED outcome)
    # + return - Error if NACK fails
    isolated remote function nack(Message message, boolean requeue = true) returns Error? = @java:Method {
        'class: "io.xlibb.solace.caller.CallerActions"
    } external;

    # Commit the current transaction.
    #
    # Only applicable when the listener connection is transacted.
    #
    # + return - Error if commit fails
    isolated remote function 'commit() returns Error? = @java:Method {
        'class: "io.xlibb.solace.caller.CallerActions"
    } external;

    # Rollback the current transaction.
    #
    # Only applicable when the listener connection is transacted.
    #
    # + return - Error if rollback fails
    isolated remote function 'rollback() returns Error? = @java:Method {
        'class: "io.xlibb.solace.caller.CallerActions"
    } external;
}
