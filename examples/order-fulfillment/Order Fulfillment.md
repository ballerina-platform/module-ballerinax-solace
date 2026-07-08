# Order Fulfillment

This example demonstrates the most fundamental Solace messaging pattern: point-to-point processing over a queue, with reliable, at-least-once delivery.

An `order_service` sends orders to an `orders` queue using a `solace:MessageProducer`. A `fulfillment_worker` pulls from the same queue with a `solace:MessageConsumer` configured with `ackMode: solace:CLIENT_ACK`, so a message stays on the queue until it is explicitly acknowledged with `consumer->ack()`. The worker simulates a crash right after receiving `ORD-1001`, closing its connection without acknowledging anything. It then opens a fresh connection - standing in for the worker restarting - and this time fulfills and acknowledges every order it receives, including the one abandoned by the "crashed" attempt.

This shows how a worker that only acknowledges a message once it has genuinely finished processing it can crash or restart without ever losing an order.

## Prerequisites

Start a local Solace broker and provision the queues used by these examples (see the [examples README](../README.md#prerequisites)):

```bash
docker compose -f ../docker-compose.yaml up -d
```

## Running the Example

Run the order service first to publish a few orders onto the queue:

```bash
cd order_service
bal run
```

Then run the fulfillment worker:

```bash
cd fulfillment_worker
bal run
```

The worker's first connection logs a "Simulating a worker crash" warning for `ORD-1001` and closes without acknowledging anything it received. Its second connection then receives every order again - logged with `redelivered=true`, since none of them were acknowledged by the abandoned first connection - and fulfills each one for good.
