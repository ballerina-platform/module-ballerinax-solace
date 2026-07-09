import ballerina/log;
import ballerinax/solace;

configurable string brokerUrl = "tcp://localhost:55554";
configurable string messageVpn = "default";
configurable string username = "admin";
configurable string password = "admin";

type PriceUpdate record {|
    string symbol;
    decimal price;
    float changePercent;
|};

public function main() returns error? {
    // No fixed destination is configured here - the producer picks a destination per publish
    // call below, so one producer can fan out to every topic instead of one-per-topic.
    solace:MessageProducer producer = check new (brokerUrl, {
        messageVpn,
        auth: {username, password}
    });

    check publish(producer, "stocks/nasdaq/aapl", {symbol: "AAPL", price: 227.50d, changePercent: 2.1});
    check publish(producer, "stocks/nasdaq/googl", {symbol: "GOOGL", price: 168.90d, changePercent: 6.8});
    check publish(producer, "stocks/nyse/ibm", {symbol: "IBM", price: 231.40d, changePercent: 9.4});

    check producer->close();
}

function publish(solace:MessageProducer producer, string topic, PriceUpdate update) returns error? {
    // Direct (at-most-once) delivery suits a live price feed: a subscriber that is not
    // connected yet or briefly falls behind should see the next update, not queue up stale ones.
    // changePercent is duplicated into properties (selectors filter on properties/headers, not
    // payload fields) so a durable topic endpoint subscriber could select on it - see the
    // example doc's Variations section.
    check producer->send({topicName: topic}, {
        payload: update,
        deliveryMode: solace:DIRECT,
        properties: {"changePercent": update.changePercent}
    });
    log:printInfo("Price update published", topic = topic, symbol = update.symbol,
            changePercent = update.changePercent);
}
