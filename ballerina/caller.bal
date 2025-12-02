import ballerina/jballerina.java;

# Caller for explicit acknowledgement and transaction control in Listener callbacks.
#
# Use this when your service's `onMessage` method needs explicit control over message
# acknowledgement or transaction operations. The Caller is provided as an optional parameter
# to the `onMessage` method.
#
# Only use Caller if your service is configured with `ackMode = "SUPPORTED_MESSAGE_ACK_CLIENT"`
# and you need manual acknowledgement control.
#
# Example with explicit acknowledgement:
# ```ballerina
# service object {} myService = @ServiceConfig {
#     queueName: "critical-orders",
#     ackMode: "SUPPORTED_MESSAGE_ACK_CLIENT"
# } service {
#     remote function onMessage(smf:Message message, smf:Caller caller) returns error? {
#         // Process the message
#         process(message);
#         // Only acknowledge after successful processing
#         check caller->acknowledge(message);
#     }
# };
# ```
#
# Example with transaction control:
# ```ballerina
# service object {} transactionService = @ServiceConfig {
#     queueName: "transactions",
#     ackMode: "SUPPORTED_MESSAGE_ACK_CLIENT"
# } service {
#     remote function onMessage(smf:Message message, smf:Caller caller) returns error? {
#         do {
#             check processTransaction(message);
#             check caller->commit();  // Commit successful transaction
#         } on fail {
#             check caller->rollback();  // Rollback on error
#         }
#     }
# };
# ```
public isolated client class Caller {

    # Acknowledge a message.
    #
    # Only use this method if the service is configured with `ackMode = "SUPPORTED_MESSAGE_ACK_CLIENT"`.
    # In AUTO mode, messages are acknowledged automatically and you should not call this method.
    #
    # + message - The message to acknowledge
    # + return - Error if acknowledgement fails
    isolated remote function acknowledge(Message message) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.caller.CallerActions",
        name: "acknowledge"
    } external;

    # Settle a message with a specific outcome.
    #
    # Available outcomes:
    # - SettlementOutcome.ACCEPTED: Acknowledge the message (positive ACK)
    # - SettlementOutcome.FAILED: Negative ACK with redelivery - message will be redelivered, delivery count incremented
    # - SettlementOutcome.REJECTED: Negative ACK without redelivery - message moved to DMQ immediately
    #
    # Only use this method if the service is configured with `ackMode = "SUPPORTED_MESSAGE_ACK_CLIENT"`
    # and the listener flow is configured to support required settlement outcomes.
    # For transacted flows, settlement outcomes are ignored.
    #
    # + message - The message to settle
    # + outcome - The settlement outcome
    # + return - Error if settlement fails
    isolated remote function settle(Message message, SettlementOutcome outcome) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.caller.CallerActions",
        name: "settle"
    } external;

    # Commit the current transaction.
    #
    # Only applicable if the listener connection is in transacted mode.
    # Commits all message operations since the last commit/rollback.
    #
    # + return - Error if commit fails
    isolated remote function 'commit() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.caller.CallerActions",
        name: "commit"
    } external;

    # Rollback the current transaction.
    #
    # Only applicable if the listener connection is in transacted mode.
    # Rolls back all message operations since the last commit/rollback.
    #
    # + return - Error if rollback fails
    isolated remote function 'rollback() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.caller.CallerActions",
        name: "rollback"
    } external;
}
