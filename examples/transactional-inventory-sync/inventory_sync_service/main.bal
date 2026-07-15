import ballerina/log;
import ballerinax/solace;

configurable string brokerUrl = "tcp://localhost:55554";
configurable string messageVpn = "default";
configurable string username = "admin";
configurable string password = "admin";

const string STOCK_UPDATES_QUEUE = "stock-updates";
const int MESSAGES_TO_PROCESS = 3;

type StockUpdate record {|
    string item;
    int delta;
|};

public function main() returns error? {
    solace:MessageConsumer consumer = check new (brokerUrl, {
        messageVpn,
        transacted: true,
        auth: {username, password},
        subscriptionConfig: {queueName: STOCK_UPDATES_QUEUE}
    });

    map<int> inventory = {"widget": 100, "gadget": 50};

    int processed = 0;
    while processed < MESSAGES_TO_PROCESS {
        record {|*solace:Message; StockUpdate payload;|}? received = check consumer->receive(10.0);
        if received is () {
            break;
        }

        StockUpdate update = received.payload;
        if !inventory.hasKey(update.item) {
            log:printWarn("Rejecting update for unknown item, rolling back transaction", item = update.item);
            check consumer->'rollback();

            // The rolled-back message is redelivered on the next receive. Discard it
            // this time so the transaction does not spin forever on the same bad update.
            record {|*solace:Message; StockUpdate payload;|}? redelivered = check consumer->receive(10.0);
            if redelivered is record {} {
                log:printWarn("Discarding redelivered update after review", item = redelivered.payload.item);
            }
            check consumer->'commit();
            processed += 1;
            continue;
        }

        inventory[update.item] = inventory.get(update.item) + update.delta;
        check consumer->'commit();
        log:printInfo("Inventory updated", item = update.item, newLevel = inventory.get(update.item));
        processed += 1;
    }

    check consumer->close();
}
