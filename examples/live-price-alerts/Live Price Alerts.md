# Live Price Alerts

This example demonstrates Solace's other core messaging model: publish/subscribe over topics, using hierarchical topic names, a wildcard subscription, and direct (at-most-once) delivery.

A `price_publisher` publishes stock price updates to topics named after the exchange and symbol, for example `stocks/nasdaq/aapl`, using `solace:DIRECT` delivery. An `alert_subscriber` service subscribes to `stocks/nasdaq/*`, so it only receives NASDAQ updates (an IBM update published to `stocks/nyse/ibm` never reaches it). Direct topic subscriptions don't support broker-side message selectors, so the subscriber filters for significant moves (`changePercent > 5.0`) itself in `onMessage`.

This shows how to narrow *which topics* a service cares about using wildcards, and highlights that filtering *which messages* matter requires application code on a direct subscription (unlike a queue or durable topic endpoint, where a `messageSelector` can push that filtering onto the broker - see Variations below).

## Prerequisites

Start a local Solace broker (see the [examples README](../README.md#prerequisites)):

```bash
docker compose -f ../docker-compose.yaml up -d
```

## Running the Example

Topic subscriptions are live - a message published while no subscriber is connected is not delivered later. Start the subscriber first:

```bash
cd alert_subscriber
bal run
```

In a separate terminal, run the publisher:

```bash
cd price_publisher
bal run
```

The publisher logs all three price updates. The subscriber only logs an alert for GOOGL (+6.8%): AAPL (+2.1%) is below the threshold, and IBM (+9.4%, well above the threshold) is never delivered at all since it's published on `stocks/nyse/ibm`, outside the `stocks/nasdaq/*` subscription.

## Variations

- **Broker-side filtering**: message selectors (`messageSelector` in `@solace:ServiceConfig`) are supported for queue and durable topic endpoint subscriptions. To push the `changePercent > 5.0` filter onto the broker instead of filtering in `onMessage`, switch the subscriber to a durable topic endpoint (`endpointType: solace:DURABLE` with an `endpointName`, pre-provisioned on the broker) and add `messageSelector: "changePercent > 5.0"` - the same property published in the message's `properties` map.
