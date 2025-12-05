import ballerina/jballerina.java;

# Listener for asynchronous (push-based) message consumption from Solace.
#
# Uses the Ballerina listener pattern for event-driven message processing. Services are
# attached to the listener and receive messages via callback methods.
#
# Supports both queue and topic subscriptions with configurable polling and auto-acknowledgement.
#
# Example queue listener:
# ```ballerina
# listener smf:Listener solaceListener = check new (
#     url = "tcp://broker:55555",
#     auth = {username: "default"}
# );
#
# service object {} queueService = @ServiceConfig {
#     queueName: "orders",
#     ackMode: "SUPPORTED_MESSAGE_ACK_AUTO"
# } service {
#     remote function onMessage(smf:Message message) returns error? {
#         io:println("Received: ", message.payload.toString());
#     }
# };
#
# check solaceListener.attach(queueService);
# check solaceListener.'start();
# ```
#
# Example topic listener with durable subscription:
# ```ballerina
# service object {} topicService = @ServiceConfig {
#     topicName: "orders/*/created",
#     endpointType: "DURABLE",
#     endpointName: "order-processor-1",
#     pollingInterval: 5,
#     receiveTimeout: 2
# } service {
#     remote function onMessage(smf:Message message) returns error? {
#         // Process message
#     }
#
#     remote function onError(smf:Error err) returns error? {
#         io:println("Error: ", err);
#     }
# };
# ```
public isolated class Listener {
    # Initialize a new Listener with the given connection configuration.
    #
    # + url - The broker URL with format: [protocol:]host[:port]
    # + config - The connection configuration (host, auth, SSL/TLS, retry, etc.)
    # + return - Error if initialization fails
    public isolated function init(string url, CommonConnectionConfiguration config) returns Error? {
        return self.initListener(url, config);
    }

    isolated function initListener(string url, CommonConnectionConfiguration config) returns Error =
    @java:Method {
        'class: "io.ballerina.lib.solace.smf.listener.ListenerActions",
        name: "init"
    } external;

    # Attach a service to the listener.
    #
    # The service must have an `onMessage` method that receives messages. The service can optionally
    # have an `onError` method for error handling.
    #
    # The service's subscription (queue or topic) is specified via the `@ServiceConfig` annotation,
    # which contains either `queueName` or `topicName` and optional polling configuration.
    #
    # + service - The service object to attach
    # + name - Optional service name for identification
    # + return - Error if attachment fails
    isolated function attach(Service 'service, string[]|string? name = ()) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.listener.ListenerActions",
        name: "attach"
    } external;

    # Detach a service from the listener.
    #
    # + service - The service object to detach
    # + return - Error if detachment fails
    isolated function detach(Service 'service) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.listener.ListenerActions",
        name: "detach"
    } external;

    # Start the listener.
    #
    # Begins polling for messages on all attached services. Services with `autoStart = true`
    # (the default) will begin receiving messages immediately.
    #
    # + return - Error if start fails
    isolated function 'start() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.listener.ListenerActions",
        name: "start"
    } external;

    # Gracefully stop the listener.
    #
    # Waits for in-flight message processing to complete before stopping.
    #
    # + return - Error if stop fails
    isolated function gracefulStop() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.listener.ListenerActions",
        name: "gracefulStop"
    } external;

    # Immediately stop the listener.
    #
    # Stops the listener without waiting for in-flight messages to complete.
    #
    # + return - Error if stop fails
    isolated function immediateStop() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.listener.ListenerActions",
        name: "immediateStop"
    } external;

}

