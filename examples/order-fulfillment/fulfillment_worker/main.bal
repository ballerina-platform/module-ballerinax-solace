import ballerina/log;
import ballerinax/solace;

configurable string brokerUrl = "tcp://localhost:55554";
configurable string messageVpn = "default";
configurable string username = "admin";
configurable string password = "admin";

const string ORDERS_QUEUE = "orders";

// Simulates a worker crash right after receiving this order, before it is acknowledged.
const string CRASH_ON_ORDER = "ORD-1001";

type Order record {|
    string orderId;
    string item;
    int quantity;
|};

public function main() returns error? {
    // First connection: simulates the worker's original process. It stops as soon as
    // it hits ORD-1001, without acknowledging it - standing in for a crash mid-fulfillment.
    log:printInfo("Starting fulfillment worker");
    check runWorkerSession();

    // Second connection: simulates the worker restarting. The unacknowledged order is
    // redelivered on this connection and completed, along with the rest of the queue.
    log:printInfo("Restarting fulfillment worker");
    check runWorkerSession();
}

function runWorkerSession() returns error? {
    solace:MessageConsumer consumer = check new (brokerUrl, {
        messageVpn,
        auth: {username, password},
        subscriptionConfig: {
            queueName: ORDERS_QUEUE,
            ackMode: solace:CLIENT_ACK
        }
    });

    while true {
        record {|*solace:Message; Order payload;|}? message = check consumer->receive(5.0);
        if message is () {
            break;
        }

        Order 'order = message.payload;
        if 'order.orderId == CRASH_ON_ORDER && message.redelivered != true {
            log:printWarn("Simulating a worker crash before fulfillment completes", orderId = 'order.orderId);
            // Leaving the message unacknowledged and closing the connection returns it
            // to the queue for redelivery to the next connection.
            break;
        }

        log:printInfo("Order fulfilled", orderId = 'order.orderId, item = 'order.item, quantity = 'order.quantity,
                redelivered = message.redelivered ?: false);
        check consumer->ack(message);
    }

    check consumer->close();
}
