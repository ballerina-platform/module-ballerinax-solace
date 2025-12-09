import ballerina/jballerina.java;

# MessageConsumer for synchronous (pull-based) message consumption from Solace.
#
# Supports both queue and topic subscriptions:
# - Queue subscriptions: guaranteed message delivery via FlowReceiver
# - Topic subscriptions: direct message delivery via XMLMessageConsumer
#
# Example queue consumer:
# ```ballerina
# final smf:MessageConsumer consumer = check new (
#     url = "tcp://broker:55555",
#     auth = {username: "default"},
#     subscriptionConfig = {
#         queueName: "orders",
#         ackMode: "SUPPORTED_MESSAGE_ACK_AUTO"
#     }
# );
# smf:Message? msg = check consumer->receive(5.0);
# ```
#
# Example topic consumer:
# ```ballerina
# final smf:MessageConsumer consumer = check new (
#     url = "tcp://broker:55555",
#     auth = {username: "default"},
#     subscriptionConfig = {
#         topicName: "orders/*/created",
#         endpointType: "DEFAULT"
#     }
# );
# smf:Message? msg = check consumer->receive(10.0);
# ```
public isolated client class MessageConsumer {

    # Initialize a new MessageConsumer with the given configuration.
    #
    # + url - The broker URL with format: [protocol:]host[:port]
    # + config - The consumer configuration (composed of connection config + subscription config)
    # + return - Error if initialization fails
    // We need to pass url here as well
    // And use *ConsumerConfiguration
public isolated function init(string url, *ConsumerConfiguration config) returns Error? {
        return self.initConsumer(url, config);
    }

    isolated function initConsumer(string url, ConsumerConfiguration config) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.consumer.ConsumerActions",
        name: "init"
    } external;

    # Receive a message with a timeout.
    #
    # Blocks up to the specified timeout waiting for a message. Returns nil if no message
    # arrives within the timeout period.
    #
    # + timeout - Maximum time in seconds to wait for a message. A timeout of zero never expires
    # + return - The received message, or nil if timeout occurs; Error if receive fails
    isolated remote function receive(decimal timeout = 0.0) returns Message|Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.consumer.ConsumerActions",
        name: "receive"
    } external;

    # Receive a message without waiting.
    #
    # Returns immediately with a message if available, or nil if no message is available.
    # This is a non-blocking operation.
    #
    # + return - The received message, or nil if no message available; Error if receive fails
    isolated remote function receiveNoWait() returns Message|Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.consumer.ConsumerActions",
        name: "receiveNoWait"
    } external;

    # Acknowledge a message in CLIENT_ACKNOWLEDGE mode.
    #
    # Only use this method if the subscription is configured with ackMode = "SUPPORTED_MESSAGE_ACK_CLIENT".
    # In AUTO mode, messages are acknowledged automatically.
    #
    # + message - The message to acknowledge
    # + return - Error if acknowledgement fails
    isolated remote function ack(Message message) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.consumer.ConsumerActions",
        name: "acknowledge"
    } external;

    # Negatively acknowledge a message (NACK).
    #
    # Sends a negative acknowledgement for the message, indicating processing failure.
    #
    # + message - The message to negatively acknowledge
    # + requeue - If true, message will be requeued for redelivery (FAILED outcome);
    # If false, message moves to DMQ immediately (REJECTED outcome)
    # + return - Error if NACK fails
    #
    # Only use this method if the subscription is configured with ackMode = "SUPPORTED_MESSAGE_ACK_CLIENT"
    # and the consumer flow is configured to support required settlement outcomes.
    # For transacted flows, settlement outcomes are ignored.
    isolated remote function nack(Message message, boolean requeue = true) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.consumer.ConsumerActions",
        name: "nack"
    } external;

    # Commit the current transaction.
    #
    # Only applicable in transacted mode. Commits all message operations since the last commit/rollback.
    #
    # + return - Error if commit fails
    isolated remote function 'commit() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.consumer.ConsumerActions",
        name: "commit"
    } external;

    # Rollback the current transaction.
    #
    # Only applicable in transacted mode. Rolls back all message operations since the last commit/rollback.
    #
    # + return - Error if rollback fails
    isolated remote function 'rollback() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.consumer.ConsumerActions",
        name: "rollback"
    } external;

    # Close the consumer and release all resources.
    #
    # + return - Error if close fails
    isolated remote function close() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.consumer.ConsumerActions",
        name: "close"
    } external;
}

