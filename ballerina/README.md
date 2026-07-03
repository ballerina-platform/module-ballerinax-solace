## Overview

[Solace PubSub+](https://docs.solace.com/) is an advanced event-broker platform that enables event-driven communication across distributed applications using multiple messaging patterns such as publish/subscribe, request/reply, and queue-based messaging. It supports standard messaging protocols, including JMS, MQTT, AMQP, and REST, enabling seamless integration across diverse systems and environments.

The `xlibb/solace` package provides APIs to interact with Solace PubSub+ brokers through the JCSMP API. It allows developers to programmatically produce and consume messages, manage topics and queues, and implement robust, event-driven solutions that leverage Solace’s high-performance messaging capabilities within Ballerina applications.

## Examples

The [examples](https://github.com/xlibb/module-solace/tree/main/examples) directory contains complete, runnable samples:

- [Order listener](https://github.com/xlibb/module-solace/tree/main/examples/order-listener) — asynchronous (push-based) message consumption from a queue using the `solace:Listener`, with explicit acknowledgement through the `solace:Caller`.
