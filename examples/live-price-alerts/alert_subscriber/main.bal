import ballerina/log;
import ballerinax/solace;

configurable string brokerUrl = "tcp://localhost:55554";
configurable string vpnName = "default";
configurable string username = "admin";
configurable string password = "admin";

type PriceUpdate record {|
    string symbol;
    decimal price;
    float changePercent;
|};

listener solace:Listener alertListener = check new (brokerUrl, {
    vpnName,
    auth: {username, password}
});

// Subscribes to every symbol under the NASDAQ topic hierarchy. Message selectors are only
// supported for queue and durable topic endpoint subscriptions, not direct topic subscriptions
// like this one, so the significant-move filter below runs in application code instead.
@solace:ServiceConfig {
    topicName: "stocks/nasdaq/*"
}
service on alertListener {

    remote function onMessage(record {|*solace:Message; PriceUpdate payload;|} message) returns error? {
        PriceUpdate update = message.payload;
        if update.changePercent <= 5.0 {
            return;
        }
        log:printWarn("ALERT: significant price move", symbol = update.symbol, price = update.price,
                changePercent = update.changePercent);
    }
}
