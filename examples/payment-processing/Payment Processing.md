# Payment Processing

This example demonstrates negative acknowledgement (`nack`): the difference between rejecting a message outright and requeuing it for another attempt.

A `payment_publisher` sends payments to a `payments` queue, including one with an invalid (negative) amount - simulating bad data from an upstream system - and one that the `payment_worker` treats as hitting a transient downstream outage on its first delivery. The worker is a `solace:Listener` service with `ackMode: solace:CLIENT_ACK`. For the invalid payment it calls `caller->nack(message, requeue = false)`, which moves the message straight to the dead message queue - retrying bad data would never make it valid. For the payment simulating a transient failure it calls `caller->nack(message, requeue = true)` instead, so the broker redelivers it; the worker succeeds on that redelivered attempt (detected via `message.redelivered`).

This shows how `nack`'s `requeue` flag lets a service distinguish "this will never succeed, stop retrying" from "this might succeed if tried again" - a distinction plain acknowledgement (or leaving a message unacknowledged, as in the [Order Fulfillment](../order-fulfillment/Order%20Fulfillment.md) example) can't express.

## Prerequisites

Start a local Solace broker (see the [examples README](../README.md#prerequisites)):

```bash
docker compose -f ../docker-compose.yaml up -d
```

## Running the Example

Run the publisher first to submit the payments:

```bash
cd payment_publisher
bal run
```

Then run the worker:

```bash
cd payment_worker
bal run
```

Expected log output: `PAY-1` is processed immediately. `PAY-2` is rejected with a warning and never seen again (moved to the dead message queue). `PAY-3` logs a "simulating a transient downstream failure" warning on its first delivery, then is processed successfully once redelivered (`redelivered=true`). The worker keeps running afterwards - press Ctrl+C to stop it.
