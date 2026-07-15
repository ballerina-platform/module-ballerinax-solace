## Overview

[Solace PubSub+](https://docs.solace.com/) is an advanced event-broker platform that enables event-driven communication across distributed applications using multiple messaging patterns such as publish/subscribe, request/reply, and queue-based messaging. It supports standard messaging protocols, including JMS, MQTT, AMQP, and REST, enabling seamless integration across diverse systems and environments.

The `ballerinax/solace` package provides APIs to interact with Solace PubSub+ brokers through the JCSMP API. It allows developers to programmatically produce and consume messages, manage topics and queues, and implement robust, event-driven solutions that leverage Solace’s high-performance messaging capabilities within Ballerina applications.

### Key Features

- Support for various messaging patterns (Pub/Sub, Point-to-Point queuing)
- Direct (at-most-once) and guaranteed (persistent) delivery modes
- Both synchronous (pull-based `MessageConsumer`) and asynchronous (push-based `Listener`) consumption
- Transacted sessions with `commit`/`rollback`
- Support for secure communication with TLS, Kerberos, and OAuth2/OIDC authentication

## Setup guide

To try out the `ballerinax/solace` connector, you need a running Solace PubSub+ broker. The quickest way to get one locally is with Docker.

### Step 1: Start a Solace PubSub+ broker with Docker

Run the following command to start a Solace PubSub+ standard broker container:

```bash
docker run -d --name solace \
    -p 55554:55555 -p 55003:55003 -p 8080:8080 \
    --shm-size=1g \
    -e username_admin_globalaccesslevel=admin \
    -e username_admin_password=admin \
    solace/solace-pubsub-standard:latest
```

Once the container is up and running, the broker is reachable at `tcp://localhost:55554` on the default message VPN `default`, with the credentials `admin`/`admin`. These are the same defaults used throughout the samples in this guide.

### Step 2: Create a queue

Unlike some other Solace client APIs, this connector does not create durable queues on demand - the samples below produce and consume messages on a queue, so create one before running them:

1. Open the Solace PubSub+ Manager at [http://localhost:8080](http://localhost:8080) and log in with `admin`/`admin`.
2. Navigate to the `default` message VPN > **Queues** and click **+ Queue**.
3. Give the queue a name (for example, `sample-queue`) and save it.

## Quickstart

### Step 1: Import the module

Import the `ballerinax/solace` module into the Ballerina project.

```ballerina
import ballerinax/solace;
```

### Step 2: Instantiate a new connector

#### Initialize a `solace:MessageProducer`

```ballerina
configurable string brokerUrl = ?;
configurable string vpnName = ?;
configurable string queueName = ?;
configurable string username = ?;
configurable string password = ?;

solace:MessageProducer producer = check new (brokerUrl,
    vpnName = vpnName,
    auth = {
        username,
        password
    }
);
```

#### Initialize a `solace:MessageConsumer`

```ballerina
configurable string brokerUrl = ?;
configurable string vpnName = ?;
configurable string queueName = ?;
configurable string username = ?;
configurable string password = ?;

solace:MessageConsumer consumer = check new (brokerUrl,
    vpnName = vpnName,
    auth = {
        username,
        password
    },
    subscriptionConfig = {
        queueName
    }
);
```

#### Initialize a `solace:Listener`

```ballerina
configurable string brokerUrl = ?;
configurable string vpnName = ?;
configurable string queueName = ?;
configurable string username = ?;
configurable string password = ?;

listener solace:Listener solaceListener = check new (brokerUrl,
    vpnName = vpnName,
    auth = {
        username,
        password
    }
);

@solace:ServiceConfig {
    queueName
}
service on solaceListener {
    remote function onMessage(solace:Message message) returns error? {
        // Process the received message
    }
}
```

### Step 3: Invoke the connector operation

Now, you can use the available connector operations to interact with the Solace broker.

#### Produce a message to a queue

```ballerina
check producer->send({
    payload: "This is a sample message"
}, {queueName});
```

#### Retrieve a message from a queue

```ballerina
solace:Message? receivedMessage = check consumer->receive(5.0);
```

A `solace:Listener` does not need to be explicitly invoked - once attached, the `onMessage` remote method is called automatically for every message on the queue as soon as the listener starts.

### Step 4: Run the Ballerina application

Save the changes and run the Ballerina application using the following command.

```bash
bal run
```

## Examples

The `ballerinax/solace` connector provides practical examples illustrating usage in various scenarios. Explore these [examples](https://github.com/ballerina-platform/module-ballerinax-solace/tree/main/examples), covering the following use cases:

1. [Order Fulfillment](https://github.com/ballerina-platform/module-ballerinax-solace/tree/main/examples/order-fulfillment) - Send orders to a queue and process them with `CLIENT_ACK` mode. Shows point-to-point (queue) messaging where a message is only acknowledged once it has been fulfilled successfully, so a worker that crashes beforehand picks it back up on restart.

2. [Live Price Alerts](https://github.com/ballerina-platform/module-ballerinax-solace/tree/main/examples/live-price-alerts) - Publish stock price updates to hierarchical topics and raise alerts only for significant moves. Shows publish/subscribe (topic) messaging with a topic wildcard and direct (at-most-once) delivery.

3. [Transactional Inventory Sync](https://github.com/ballerina-platform/module-ballerinax-solace/tree/main/examples/transactional-inventory-sync) - Apply inventory deltas from a queue within a transacted session, rolling back and safely discarding a bad update instead of corrupting inventory state. Shows transacted consumption with `commit`/`rollback`.

4. [Payment Processing](https://github.com/ballerina-platform/module-ballerinax-solace/tree/main/examples/payment-processing) - Reject an invalid payment outright while retrying one that hits a simulated transient failure. Shows negative acknowledgement (`nack`) and the difference between rejecting a message to the dead message queue and requeuing it for redelivery.
