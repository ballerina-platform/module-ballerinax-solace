## Overview

Solace PubSub+ is a powerful event broker that supports multiple protocols and messaging patterns. It provides high-performance, reliable, and scalable messaging for modern event-driven architectures. The Solace connector allows you to integrate with Solace event brokers, enabling efficient event distribution across various environments.

### Key Features

- Support for various messaging patterns (Pub/Sub, Request-Reply, Queuing)
- Seamless integration with Solace PubSub+ event brokers
- High-performance event distribution and reliable message delivery
- Support for secure communication with TLS and authentication
- Simplified production and consumption of events
- GraalVM compatible for native image builds

[Solace PubSub+](https://docs.solace.com/) is an advanced event-broker platform that enables event-driven communication across distributed applications using multiple messaging patterns such as publish/subscribe, request/reply, and queue-based messaging. It supports standard messaging protocols, including JMS, MQTT, AMQP, and REST, enabling seamless integration across diverse systems and environments.

The `ballerinax/solace` package provides APIs to interact with Solace PubSub+ brokers through the JMS API. It allows developers to programmatically produce and consume messages, manage topics and queues, and implement robust, event-driven solutions that leverage Solace’s high-performance messaging capabilities within Ballerina applications.

## Quickstart

### Step 1: Import the module

Import the `solace` module into the Ballerina project.

```ballerina
import ballerinax/solace;
```

### Step 2: Instantiate a new connector

#### Initialize a `solace:MessageProducer`

```ballerina
configurable string brokerUrl = ?;
configurable string messageVpn = ?;
configurable string queueName = ?;
configurable string username = ?;
configurable string password = ?;

solace:MessageProducer producer = check new (brokerUrl,
    destination = {
        queueName
    },
    messageVpn = messageVpn,
    auth = {
        username,
        password
    }
);
```

#### Initialize a `solace:MessageConsumer`

```ballerina
configurable string brokerUrl = ?;
configurable string messageVpn = ?;
configurable string queueName = ?;
configurable string username = ?;
configurable string password = ?;

solace:MessageConsumer consumer = check new (brokerUrl,
    destination = {
        queueName
    },
    messageVpn = messageVpn,
    auth = {
        username,
        password
    }
);
```

### Step 3: Invoke the connector operation

Now, you can use the available connector operations to interact with Solace broker.

#### Produce message to a queue

```ballerina
check producer->send({
    payload: "This is a sample message"
});
```

#### Retrieve a message from a queue

```ballerina
solace:Message? receivedMessage = check consumer->receive(5.0);
```

### Step 4: Run the Ballerina application

Save the changes and run the Ballerina application using the following command.

```bash
bal run
```

