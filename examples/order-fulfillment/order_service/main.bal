import ballerina/log;
import ballerinax/solace;

configurable string brokerUrl = "tcp://localhost:55554";
configurable string messageVpn = "default";
configurable string username = "admin";
configurable string password = "admin";

const string ORDERS_QUEUE = "orders";

type Order record {|
    string orderId;
    string item;
    int quantity;
|};

public function main() returns error? {
    solace:MessageProducer producer = check new (brokerUrl, {
        messageVpn,
        auth: {username, password}
    });

    Order[] orders = [
        {orderId: "ORD-1001", item: "Wireless Mouse", quantity: 2},
        {orderId: "ORD-1002", item: "Mechanical Keyboard", quantity: 1},
        {orderId: "ORD-1003", item: "USB-C Hub", quantity: 3}
    ];

    foreach Order 'order in orders {
        check producer->send({
            payload: 'order,
            deliveryMode: solace:PERSISTENT
        }, {queueName: ORDERS_QUEUE});
        log:printInfo("Order placed", orderId = 'order.orderId, item = 'order.item);
    }

    check producer->close();
}
