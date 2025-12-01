// Destination types - Topic and Queue
public type Topic record {
    string name;
};

public type Queue record {
    string name;
};

public type Destination Topic|Queue;

// Common connection configuration shared between producer and consumer
public type CommonConnectionConfiguration record {|
    string host;
    int port = 55555;
    string username;
    string password;
    string vpnName = "default";
    string transportProtocol = "PLAIN_TEXT";
    decimal connectionTimeout = 30.0;
    decimal readTimeout = 10.0;
|};

// Producer-specific configuration
public type ProducerConfiguration record {|
    *CommonConnectionConfiguration;
    Destination? destination = ();
|};

// Message type for publishing
public type Message record {|
    string|byte[]|map<anydata> payload;
    map<anydata> properties?;
|};

// Delivery modes for message publishing
public enum DeliveryMode {
    DIRECT,
    PERSISTENT,
    NON_PERSISTENT
}
