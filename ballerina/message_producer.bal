import ballerina/jballerina.java;

// Isolated MessageProducer client class for thread-safe message publishing
public isolated client class MessageProducer {
    // Initialize the message producer with connection configuration
    public isolated function init(*ProducerConfiguration config) returns error? {
        return externInit(self, config);
    }

    // Send a message to the specified destination
    // If destination is not provided, uses the configured default destination
    isolated remote function send(Message message, Destination? destination = ()) returns error? {
        map<anydata> destMap = destination is null ? {} : destination;
        return externSend(self, message, destMap);
    }

    // Close the producer and release resources
    isolated remote function close() returns error? {
        return externClose(self);
    }
}

// External function bindings to Java implementation
isolated function externInit(MessageProducer producer, map<anydata> config) returns error? = @java:Method {
    name: "init",
    'class: "io.ballerina.lib.solace.smf.producer.ProducerActions"
} external;

isolated function externSend(MessageProducer producer, map<anydata> message, map<anydata> destination) returns error? = @java:Method {
    name: "send",
    'class: "io.ballerina.lib.solace.smf.producer.ProducerActions"
} external;

isolated function externClose(MessageProducer producer) returns error? = @java:Method {
    name: "close",
    'class: "io.ballerina.lib.solace.smf.producer.ProducerActions"
} external;
