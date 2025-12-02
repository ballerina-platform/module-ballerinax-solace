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
    # + host - The broker host URL with format: [protocol:]host[:port]
    # + config - The producer connection configuration
    # + return - Error if initialization fails
    isolated function init(string host, *ProducerConfiguration config) returns Error? {
        return self.initProducer(host, config);
    }

    isolated function initProducer(string host, ProducerConfiguration config) returns Error = @java:Method {
        'class: "io.ballerina.lib.solace.smf.producer.ProducerActions",
        name: "init"
    } external;

    # Send a message to the specified destination.
    #
    # The message's delivery mode determines the guarantee level:
    # - DIRECT: At-most-once delivery (no guarantees)
    # - PERSISTENT: Once-and-only-once delivery with broker acknowledgement
    # - NON_PERSISTENT: Once-and-only-once delivery (functionally equivalent to PERSISTENT)
    #
    # + message - The message to send (payload and optional properties)
    # + destination - The destination to send to (topic or queue)
    # + return - Error if send fails
    isolated remote function send(Message message, Destination destination) returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.producer.ProducerActions",
        name: "send"
    } external;

    # Check if the producer is closed.
    #
    # + return - True if the producer is closed, false otherwise
    isolated remote function isClosed() returns boolean = @java:Method {
        'class: "io.ballerina.lib.solace.smf.producer.ProducerActions",
        name: "isClosed"
    } external;

    # Close the producer and release all resources.
    #
    # After calling this method, the producer cannot be used.
    #
    # + return - Error if close fails
    isolated remote function close() returns Error? = @java:Method {
        'class: "io.ballerina.lib.solace.smf.producer.ProducerActions",
        name: "close"
    } external;
}

