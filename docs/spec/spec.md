# Specification: Ballerina `ibm.ibmmq` Library

_Authors_: @ayeshLK \
_Reviewers_: TBA \
_Created_: 2025/11/21 \
_Updated_: 2025/11/23 \
_Edition_: Swan Lake

## Introduction

This is the specification for the `solace` library of [Ballerina language](https://ballerina.io/), which provides the
functionality to send and receive messages by connecting to a Solace Event Broker via JMS protocol.

The `solace` library specification has evolved and may continue to evolve in the future. The released versions of the
specification can be found under the relevant GitHub tag.

If you have any feedback or suggestions about the library, start a discussion via a GitHub issue or in the Discord
server. Based on the outcome of the discussion, the specification and implementation can be updated. Community feedback
is always welcome. Any accepted proposal which affects the specification is stored under `/docs/proposals`. Proposals
under discussion can be found with the label `type/proposal` in Github.

The conforming implementation of the specification is released to Ballerina Central. Any deviation from the specification is considered a bug.

// todo: update this section properly

## Contents

1. [Overview](#1-overview)
2. [Queue Manager](#2-queue-manager)
    * 2.1. [Configurations](#21-configurations)
    * 2.2. [Initialization](#22-initialization)
    * 2.3. [Functions](#23-functions)
3. [Message](#3-message)
4. [Client Options](#4-client-options)
5. [Queue](#5-queue)
    * 5.1. [Functions](#51-functions)
6. [Topic](#6-topic)
    * 6.1. [Functions](#61-functions)
7. [Message listener](#7-message-listener)
   * 7.1. [Initialization](#71-initialization)
   * 7.2. [Functions](#72-functions)
   * 7.3. [Service](#73-service)
     * 7.3.1. [Configuration](#731-configuration)
     * 7.3.2. [Functions](#732-functions)
   * 7.4. [Caller](#74-caller)
     * 7.4.1. [Functions](#741-functions)
   * 7.5. [Usage](#75-usage)

## 1. Overview

Solace Event Broker is a high-performance event-streaming and messaging platform that enables real-time, scalable, and event-driven communication between distributed applications. This specification describes how to use JMS API based clients to connect to Solace event broker. These clients allow the writing of distributed applications and microservices that read, write, and process messages in parallel, at scale, and in a fault-tolerant manner even in the case of network problems or machine failures.

Ballerina `solace` provides several core APIs:

- **`solace:MessageProducer`**: A client endpoint for sending messages to a Solace queue or topic.
- **`solace:MessageConsumer`**: A client endpoint for receiving messages from a Solace queue or topic.
- **`solace:Listener`**: An endpoint that allows a Ballerina service to receive messages from a Solace queue or topic.
- **`solace:Caller`**: A client used within a service to acknowledge messages or manage transactions.

## 2. CommonConfigurations

- `CommonConnectionConfiguration` record represents the common configurations needed for connecting with the Solace event broker.

```ballerina
type CommonConnectionConfiguration record {
    # The name of the message VPN to connect to
    string messageVpn = "default";
    # The authentication configuration. Supports basic authentication, Kerberos, and OAuth2.
    # For client certificate authentication, configure the `secureSocket.keyStore` field
    BasicAuthConfig|KerberosConfig|OAuth2Config auth?;
    # The SSL/TLS configuration for secure connections
    SecureSocket secureSocket?;
    # Enables transacted messaging when set to `true`. In transacted mode, messages are sent and received
    # within a transaction context, requiring explicit commit or rollback
    boolean transacted = false;
    # The client identifier. If not specified, a unique client ID is auto-generated
    string clientId?;
    # A description for the application client
    string clientDescription = "JNDI";
    # Specifies whether to allow the same client ID to be used across multiple connections
    boolean allowDuplicateClientId = false;
    # Enables automatic creation of durable queues and topic endpoints on the broker
    boolean enableDynamicDurables = false;
    # Enables direct transport mode for message delivery. When `true`, uses direct (at-most-once) delivery.
    # When `false`, uses guaranteed (persistent) delivery mode. Direct transport must be disabled for
    # transacted sessions and XA transactions.
    boolean directTransport = true;
    # Enables direct message optimization. When `true`, optimizes message delivery in direct transport mode
    # by reducing protocol overhead. Only applicable when `directTransport` is `true`.
    boolean directOptimized = true;
    # The local interface IP address to bind for outbound connections
    string localhost?;
    # The the maximum amount of time (in seconds) permitted for a JNDI connection attempt.
    # A value of 0 means wait indefinitely
    decimal connectTimeout = 30.0;
    # the maximum amount of time (in seconds) permitted for reading a JNDI lookup reply from the host
    decimal readTimeout = 10.0;
    # The configuration to enable and specify the ZLIB compression level.
    # Valid range is 0-9, where 0 means no compression. Higher values provide better compression at the slower throughput
    int compressionLevel = 0;
    # The retry configuration for connection and reconnection attempts
    RetryConfig retryConfig?;
};
```

- `BasicAuthConfig` record represents the basic authentication credentials for connecting to a Solace broker.

```ballerina
public type BasicAuthConfig record {|
    # The username for authentication
    string username;
    # The password for authentication
    string password?;
|};
```

- `KerberosConfig` record represents the Kerberos (GSS-KRB) authentication configuration for connecting to a Solace broker. 

```ballerina
public type KerberosConfig record {|
    # The Kerberos service name used during authentication
    string serviceName = "solace";
    # The JAAS login context name to use for authentication
    string jaasLoginContext = "SolaceGSS";
    # Specifies whether to enable Kerberos mutual authentication
    boolean mutualAuthentication = true;
    # Specifies whether to enable automatic reload of the JAAS configuration file
    boolean jaasConfigReloadEnabled = false;
|};
```

- `OAuth2Config` record represents the OAuth 2.0 authentication configuration for connecting to a Solace broker. 

```ballerina
public type OAuth2Config record {|
    # The OAuth 2.0 issuer identifier URI
    string issuer;
    # The OAuth 2.0 access token for authentication
    string accessToken?;
    # The OpenID Connect (OIDC) ID token for authentication
    string oidcToken?;
|};
```

- `SecureSocket` record represents the SSL/TLS configuration for secure connections to a Solace broker.

```ballerina
public type SecureSocket record {|
    # The trust store configuration containing trusted CA certificates
    TrustStore trustStore?;
    # The key store configuration containing the client's private key and certificate.
    # When configured, enables client certificate authentication
    KeyStore keyStore?;
    # The list of SSL/TLS protocol versions to enable for the connection.
    # It is recommended to use only TLSv12 or higher for security
    Protocol[] protocols = [SSLv30, TLSv10, TLSv11, TLSv12];
    # The list of cipher suites to enable for the connection.
    # If not specified, the default cipher suites for the JVM are used
    SslCipherSuite[] cipherSuites?;
    # The list of acceptable common names for broker certificate validation.
    # If specified, the broker certificate's common name must match one of these values
    string[] trustedCommonNames?;
    # The certificate validation settings
    record {|
        # Enable certificate validation
        boolean enabled = true;
        # Specifies whether to validate the certificate's expiration date
        boolean validateDate = true;
        # Specifies whether to validate that the certificate's common name matches the broker hostname
        boolean validateHost = true;
    |} validation = {};
|};
```

- `TrustStore` record represents a trust store containing trusted CA certificates.

```ballerina
public type TrustStore record {|
    # The URL or path of the truststore file
    string location;
    # The password for the trust store
    string password;
    # The format of the trust store file
    SslStoreFormat format = JKS;
|};
```

- `KeyStore` record represents a key store containing the client's private key and certificate. 

```ballerina
public type KeyStore record {|
    # The URL or path of the keystore file
    string location;
    # The password for the key store
    string password;
    # The password for the private key within the key store.
    # If not specified, the key store password is used
    string keyPassword?;
    # The alias of the private key to use from the key store.
    # If not specified, the first private key found is used
    string keyAlias?;
    # The format of the key store file
    SslStoreFormat format = JKS;
|};
```

- `Protocol` type represents the supported SSL/TLS protocol versions.

```ballerina
public type Protocol SSLv30|TLSv10|TLSv11|TLSv12;
```

- `SslCipherSuite` type represents the SSL Cipher Suite to be used for secure communication with the Solace broker.

```ballerina
public type SslCipherSuite ECDHE_RSA_AES256_CBC_SHA384|ECDHE_RSA_AES256_CBC_SHA|RSA_AES256_CBC_SHA256|RSA_AES256_CBC_SHA|
    ECDHE_RSA_3DES_EDE_CBC_SHA|RSA_3DES_EDE_CBC_SHA|ECDHE_RSA_AES128_CBC_SHA|ECDHE_RSA_AES128_CBC_SHA256|RSA_AES128_CBC_SHA256|
    RSA_AES128_CBC_SHA;
```

- `RetryConfig` record represents the retry configuration for connection and reconnection attempts to a Solace broker. 

```ballerina
public type RetryConfig record {|
    # The number of times to retry connecting to the broker during initial connection.
    # A value of -1 means retry forever, 0 means no retries (fail immediately on first failure)
    int connectRetries = 0;
    # The number of connection retries per host when multiple hosts are specified in the URL.
    # This applies to each host in a comma-separated host list
    int connectRetriesPerHost = 0;
    # The number of times to retry reconnecting after an established connection is lost.
    # A value of -1 means retry forever
    int reconnectRetries = 20;
    # The time to wait between reconnection attempts, in seconds
    decimal reconnectRetryWait = 3.0;
|};
```

## 3. Message

An Solace message is a fundamental unit of data that facilitates communication between applications and the Solace event broker. It encompasses not only the actual data payload but also includes metadata in the form of headers and customizable properties. This comprehensive structure enables reliable, secure, and flexible data transfer in distributed and enterprise environments.

- `Message` record represent the message used to send and receive content from the Solace broker.

```ballerina
public type Message record {|
    # Message payload
    anydata payload;
    # Id which can be used to correlate multiple messages
    string correlationId?;
    # JMS destination to which a reply to this message should be sent
    Destination replyTo?;
    # Additional message properties
    map<Property> properties?;
    # Unique identifier for a JMS message (Only set by the JMS provider)
    string messageId?;
    # Time a message was handed off to a provider to be sent (Only set by the JMS provider)
    int timestamp?;
    # JMS destination of this message (Only set by the JMS provider)
    Destination destination?;
    # Delivery mode of this message (Only set by the JMS provider)
    int deliveryMode?;
    # Indication of whether this message is being redelivered (Only set by the JMS provider)
    boolean redelivered?;
    # Message type identifier supplied by the client when the message was sent
    string jmsType?;
    # Message expiration time (Only set by the JMS provider)
    int expiration?;
    # Message priority level (Only set by the JMS provider)
    int priority?;
|};
```

- `Destination` type represents a message destination in Solace.

```ballerina
public type Destination Topic|Queue;

# Represents a topic destination for publish/subscribe messaging.
public type Topic record {|
    # The name of the topic. Topics support wildcard subscriptions and multi-level hierarchies 
    # using '/' as a delimiter (e.g., "orders/retail/usa")
    string topicName;
|};

# Represents a queue destination for point-to-point messaging.
public type Queue record {|
    # The name of the queue
    string queueName;
|};
```

- `Property` type represent the valid value types allowed in JMS message properties.

```ballerina
public type Property boolean|int|byte|float|string;
```

## 4. MessageProducer

The `solace:MessageProducer` is used to send messages to a Solace destination.

### 4.1 Configurations

- `ProducerConfiguration` record represents the configuration for a Solace message producer.

```ballerina
public type ProducerConfiguration record {|
    *solace:CommonConnectionConfiguration;
    # The destination (Topic or Queue) where messages will be published
    Destination destination;
|};
```

### 4.2. Initialization

- The `solace:MessageProducer` can be initialized by providing the broker URL and the `solace:ProducerConfiguration`.

```ballerina
# Initializes a new Solace message producer with the given broker URL and configuration.
# ```
# solace:MessageProducer producer = check new (brokerUrl, {
#     destination: {queueName: "orders"},
#     transacted: false
# });
# ```
#
# + url - The Solace broker URL in the format `<scheme>://[username]:[password]@<host>[:port]`.
# Supported schemes are `smf` (plain-text) and `smfs` (TLS/SSL).
# Multiple hosts can be specified as a comma-separated list for failover support.
# Default ports: 55555 (standard), 55003 (compression), 55443 (SSL)
# + config - Producer configuration including connection settings and destination
# + return - A `solace:Error` if initialization fails or else `()`
public isolated function init(string url, *ProducerConfiguration config) returns Error?;
```

### 4.3. Functions

- To send a message to a destination in the Solace event broker, use `send` function.

```ballerina
# Sends a message to the Solace broker.
# ```
# check producer->send(message);
# ```
#
# + message - Message to be sent to the Solace broker
# + return - A `solace:Error` if there is an error or else `()`
isolated remote function send(Message message) returns Error?;
```

- To commit all messages sent in this transaction and releases any locks currently held, use the `commit` function.

```ballerina
# Commits all messages sent in this transaction and releases any locks currently held.
# This method should only be called when the producer is configured with `transacted: true`.
# ```
# check producer->'commit();
# ```
#
# + return - A `solace:Error` if there is an error or else `()`
isolated remote function 'commit() returns Error?;
```

- To roll back any messages sent in this transaction and releases any locks currently held, use the `rollback` function.

```ballerina
# Rolls back any messages sent in this transaction and releases any locks currently held.
# This method should only be called when the producer is configured with `transacted: true`.
# ```
# check producer->'rollback();
# ```
#
# + return - A `solace:Error` if there is an error or else `()`
isolated remote function 'rollback() returns Error?;
```

- To close the connection to the message broker and release any underlying resources currently help, use the `close` function.

```ballerina
# Closes the message producer.
# ```
# check producer->close();
# ```
# + return - A `solace:Error` if there is an error or else `()`
isolated remote function close() returns Error?;
```

## 5. MessageConsumer

#### 3.1.1. Initialization

The producer is initialized with the broker URL and a `ProducerConfiguration` record.

```ballerina
solace:MessageProducer producer = check new (brokerUrl, {
    destination: {topicName: "MyTopic"},
    auth: {
        username: "myuser",
        password: "mypassword"
    }
});
```

- **`url`** (string): The Solace broker URL.
- **`config`** (`ProducerConfiguration`): The configuration for the producer.

#### 3.1.2. Remote Functions

- **`send(solace:Message message)`**: Sends a message to the configured destination.
  - **`message`** (`solace:Message`): The message to be sent.
  - **Returns**: `solace:Error?`

- **`commit()`**: Commits the current transaction.
  - **Returns**: `solace:Error?`

- **`rollback()`**: Rolls back the current transaction.
  - **Returns**: `solace:Error?`

- **`close()`**: Closes the producer.
  - **Returns**: `solace:Error?`

### 3.2. `solace:MessageConsumer`

The `solace:MessageConsumer` is used to receive messages from a Solace destination.

#### 3.2.1. Initialization

The consumer is initialized with the broker URL and a `ConsumerConfiguration` record.

```ballerina
solace:MessageConsumer consumer = check new (brokerUrl, {
    subscriptionConfig: {queueName: "MyQueue"},
    auth: {
        username: "myuser",
        password: "mypassword"
    }
});
```

- **`url`** (string): The Solace broker URL.
- **`config`** (`ConsumerConfiguration`): The configuration for the consumer.

#### 3.2.2. Remote Functions

- **`receive(decimal timeout = 10.0)`**: Receives a message, waiting for a specified timeout.
  - **`timeout`** (decimal): The timeout in seconds.
  - **Returns**: `solace:Message|solace:Error?`

- **`receiveNoWait()`**: Receives a message without waiting.
  - **Returns**: `solace:Message|solace:Error?`

- **`acknowledge(solace:Message message)`**: Acknowledges a message.
  - **`message`** (`solace:Message`): The message to acknowledge.
  - **Returns**: `solace:Error?`

- **`commit()`**: Commits the current transaction.
  - **Returns**: `solace:Error?`

- **`rollback()`**: Rolls back the current transaction.
  - **Returns**: `solace:Error?`

- **`close()`**: Closes the consumer.
  - **Returns**: `solace:Error?`

### 3.3. `solace:Caller`

The `solace:Caller` is used inside a `solace:Service` to acknowledge a message or to handle transactions.

#### 3.3.1. Remote Functions

- **`acknowledge(solace:Message message)`**: Acknowledges a message.
  - **`message`** (`solace:Message`): The message to acknowledge.
  - **Returns**: `solace:Error?`

- **`commit()`**: Commits the current transaction.
  - **Returns**: `solace:Error?`

- **`rollback()`**: Rolls back the current transaction.
  - **Returns**: `solace:Error?`

## 4. Listener Endpoint

The `solace:Listener` is used to receive messages from a Solace destination and dispatch them to a service.

### 4.1. Initialization

The listener is initialized with the broker URL and a `ListenerConfiguration` record.

```ballerina
listener solace:Listener messageListener = check new (
    url = "smf://localhost:55554",
    messageVpn = "default",
    auth = {
        username: "admin",
        password: "admin"
    }
);
```

- **`url`** (string): The Solace broker URL.
- **`config`** (`ListenerConfiguration`): The configuration for the listener.

### 4.2. Service

A service can be attached to the listener to process incoming messages.

```ballerina
@solace:ServiceConfig {
    queueName: "MyQueue"
}
service solace:Service on messageListener {
    remote function onMessage(solace:Message message, solace:Caller caller) returns error? {
        // Process message
    }
}
```

The `solace:ServiceConfig` annotation is used to configure the service.

## 5. Data Types

### 5.1. `solace:Message`

Represents a Solace message.

| Field           | Type                     | Description                               |
|-----------------|--------------------------|-------------------------------------------|
| `payload`       | `anydata`                | The message payload.                      |
| `correlationId` | `string?`                | The correlation ID for the message.       |
| `replyTo`       | `solace:Destination?`    | The destination for replies.              |
| `properties`    | `map<solace:Property>?`  | Custom message properties.                |
| `messageId`     | `string?`                | The message ID.                           |
| `timestamp`     | `int?`                   | The message timestamp.                    |
| `destination`   | `solace:Destination?`    | The destination of the message.           |
| `deliveryMode`  | `int?`                   | The delivery mode.                        |
| `redelivered`   | `boolean?`               | Whether the message is redelivered.       |
| `jmsType`       | `string?`                | The JMS message type.                     |
| `expiration`    | `int?`                   | The message expiration time.              |
| `priority`      | `int?`                   | The message priority.                     |

### 5.2. `solace:Destination`

Represents a message destination, which can be a `solace:Topic` or a `solace:Queue`.

- **`solace:Topic`**: `record {| string topicName; |}`
- **`solace:Queue`**: `record {| string queueName; |}`

### 5.3. Configurations

The connector uses several record types for configuration:

- `ProducerConfiguration`
- `ConsumerConfiguration`
- `ListenerConfiguration`
- `CommonConnectionConfiguration`
- `BasicAuthConfig`
- `KerberosConfig`
- `OAuth2Config`
- `RetryConfig`
- `SecureSocket`
- `TrustStore`
- `KeyStore`
- `QueueConfig`
- `TopicConfig`
- `QueueServiceConfig`
- `TopicServiceConfig`
- `CommonSubscriptionConfig`
- `CommonServiceConfig`

### 5.4. Enums

- **`AcknowledgementMode`**: `SESSION_TRANSACTED`, `AUTO_ACKNOWLEDGE`, `CLIENT_ACKNOWLEDGE`, `DUPS_OK_ACKNOWLEDGE`
- **`ConsumerType`**: `DURABLE`, `DEFAULT`
- **`Protocol`**: `SSLv30`, `TLSv10`, `TLSv11`, `TLSv12`
- **`SslCipherSuite`**: Various cipher suites.
- **`SslStoreFormat`**: `JKS`, `PKCS12`

## 6. Error Handling

The connector functions return a `solace:Error` on failure. This is a distinct error type that provides details about the error that occurred.

This specification provides a high-level overview of the Ballerina Solace connector. For more detailed information, please refer to the connector's API documentation.
