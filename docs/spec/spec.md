
# Ballerina Solace Connector Specification

This document provides a specification for the Ballerina Solace connector.

## 1. Overview

The Ballerina Solace connector allows you to connect to the Solace PubSub+ event broker and perform messaging operations such as producing and consuming messages from queues and topics. It supports various authentication mechanisms, SSL/TLS for secure communication, and transacted sessions.

## 2. Connector Components

The connector consists of the following main components:

- **`solace:MessageProducer`**: A client endpoint for sending messages to a Solace queue or topic.
- **`solace:MessageConsumer`**: A client endpoint for receiving messages from a Solace queue or topic.
- **`solace:Listener`**: An endpoint that allows a Ballerina service to receive messages from a Solace queue or topic.
- **`solace:Caller`**: A client used within a service to acknowledge messages or manage transactions.

## 3. Client Endpoints

### 3.1. `solace:MessageProducer`

The `solace:MessageProducer` is used to send messages to a Solace destination.

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
