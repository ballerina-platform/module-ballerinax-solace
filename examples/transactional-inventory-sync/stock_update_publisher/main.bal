import ballerina/log;
import ballerinax/solace;

configurable string brokerUrl = "tcp://localhost:55554";
configurable string vpnName = "default";
configurable string username = "admin";
configurable string password = "admin";

const string STOCK_UPDATES_QUEUE = "stock-updates";

type StockUpdate record {|
    string item;
    int delta;
|};

public function main() returns error? {
    solace:MessageProducer producer = check new (brokerUrl, {
        vpnName,
        auth: {username, password}
    });

    // "gizmo" is not a known item - it simulates a bad update from a misconfigured
    // upstream system, which the sync service must reject without corrupting inventory.
    StockUpdate[] updates = [
        {item: "widget", delta: -10},
        {item: "gizmo", delta: 5},
        {item: "gadget", delta: 20}
    ];

    foreach StockUpdate update in updates {
        check producer->send({queueName: STOCK_UPDATES_QUEUE}, {
            payload: update,
            deliveryMode: solace:PERSISTENT
        });
        log:printInfo("Stock update sent", item = update.item, delta = update.delta);
    }

    check producer->close();
}
