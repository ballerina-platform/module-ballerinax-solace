# Transactional Inventory Sync

This example demonstrates transacted consumption: applying a batch of related changes as a unit of work that can be rolled back, instead of relying on per-message acknowledgement alone.

A `stock_update_publisher` seeds a `stock-updates` queue with inventory delta messages, including one for an item ("gizmo") that doesn't exist in inventory - simulating bad data from a misconfigured upstream system. An `inventory_sync_service` consumes with `transacted: true` on its `solace:MessageConsumer`. For each valid update it applies the delta to an in-memory inventory map and calls `consumer->'commit()`. For the unknown item, it calls `consumer->'rollback()` instead of applying anything - the message is redelivered, and this time the service logs and discards it (then commits), so the transaction doesn't spin forever on the same bad update.

The result: inventory only ever reflects fully-applied, valid updates. A bad message is never partially applied and never silently dropped without being redelivered at least once for review.

**Note on scope:** this module opens an independent connection and session per `MessageProducer`/`MessageConsumer`/`Listener` instance, so a transaction is always local to a single one of them. There's no way to, for example, receive a message and publish a downstream confirmation as one atomic transaction across a consumer and a producer - each side commits or rolls back independently. This example only demonstrates transactional guarantees on the consuming side.

## Prerequisites

Start a local Solace broker (see the [examples README](../README.md#prerequisites)):

```bash
docker compose -f ../docker-compose.yaml up -d
```

## Running the Example

Seed the queue first:

```bash
cd stock_update_publisher
bal run
```

Then run the sync service:

```bash
cd inventory_sync_service
bal run
```

You should see `widget` and `gadget` updated successfully, and a pair of "unknown item" log lines for `gizmo` - one for the initial rollback, one for the redelivered copy being discarded.
