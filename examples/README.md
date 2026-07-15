# Examples

The `ballerinax/solace` connector provides practical examples illustrating usage in various scenarios. Explore these [examples](https://github.com/ballerina-platform/module-ballerinax-solace/tree/main/examples) to understand how to produce, consume, and reliably process messages with a Solace event broker.

1. [Order Fulfillment](order-fulfillment/Order%20Fulfillment.md) - Send orders to a queue and process them with `CLIENT_ACK` mode. Shows point-to-point (queue) messaging where a message is only acknowledged once it has been fulfilled successfully, so a worker that crashes beforehand picks it back up on restart.

2. [Live Price Alerts](live-price-alerts/Live%20Price%20Alerts.md) - Publish stock price updates to hierarchical topics and raise alerts only for significant moves. Shows publish/subscribe (topic) messaging with a topic wildcard and direct (at-most-once) delivery.

3. [Transactional Inventory Sync](transactional-inventory-sync/Transactional%20Inventory%20Sync.md) - Apply inventory deltas from a queue within a transacted session, rolling back and safely discarding a bad update instead of corrupting inventory state. Shows transacted consumption with `commit`/`rollback`.

4. [Payment Processing](payment-processing/Payment%20Processing.md) - Reject an invalid payment outright while retrying one that hits a simulated transient failure. Shows negative acknowledgement (`nack`) and the difference between rejecting a message to the dead message queue and requeuing it for redelivery.

## Prerequisites

All examples connect to a local Solace PubSub+ broker. Start one with Docker:

```bash
docker compose -f examples/docker-compose.yaml up -d
```

This starts a broker reachable at `tcp://localhost:55554`, message VPN `default`, with credentials `admin`/`admin` - the defaults every example already uses.

Wait until the message VPN is operational (takes ~30 seconds - SEMP responds before guaranteed messaging is ready):

```bash
until [ "$(curl -s -u admin:admin \
  'http://localhost:8080/SEMP/v2/monitor/msgVpns/default?select=state' \
  | grep -o '"state":"[^"]*"')" = '"state":"up"' ]; do echo "waiting for VPN..."; sleep 3; done
echo "broker ready"
```

Unlike `solace.jms`, this connector has no dynamic-durable-queue creation, so the queues used by the examples must be provisioned up front via the SEMP REST API:

```bash
for queue in orders stock-updates payments; do
  curl -X POST -u admin:admin -H 'Content-Type: application/json' \
    http://localhost:8080/SEMP/v2/config/msgVpns/default/queues \
    -d "{\"queueName\":\"$queue\",\"accessType\":\"exclusive\",\"permission\":\"delete\",\"ingressEnabled\":true,\"egressEnabled\":true}"
done
```

(With Solace Cloud, create these three queues from the broker console instead, and point each example at your broker via its own `Config.toml` - see the individual example docs.)

## Running an Example

Each example is made up of one or more independent Ballerina projects (for example, a producer and a consumer/worker). Build and run each one from its own directory:

```bash
bal run
```

Where an example has more than one project, run them in the order given in that example's own doc - it varies depending on whether the messaging is queue-based (durable, so order usually doesn't matter) or topic-based (live, so the subscriber must be running first).

## Building the examples with the local module

**Warning**: Due to the absence of support for reading local repositories for single Ballerina files, the Bala of the module is manually written to the central repository as a workaround. Consequently, the bash script may modify your local Ballerina repositories.

Execute the following commands to build all the examples against the changes you have made to the module locally:

* To build all the examples:

    ```bash
    ./build.sh build
    ```

* To run all the examples:

    ```bash
    ./build.sh run
    ```
