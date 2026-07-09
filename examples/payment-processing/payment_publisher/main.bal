import ballerina/log;
import ballerinax/solace;

configurable string brokerUrl = "tcp://localhost:55554";
configurable string messageVpn = "default";
configurable string username = "admin";
configurable string password = "admin";

const string PAYMENTS_QUEUE = "payments";

type Payment record {|
    string paymentId;
    decimal amount;
|};

public function main() returns error? {
    solace:MessageProducer producer = check new (brokerUrl, {
        messageVpn,
        auth: {username, password}
    });

    // PAY-2 has a negative amount - simulates bad data from an upstream system, which the
    // worker must reject outright instead of retrying. PAY-3 is otherwise valid but the worker
    // simulates a transient downstream outage on its first delivery.
    Payment[] payments = [
        {paymentId: "PAY-1", amount: 49.99d},
        {paymentId: "PAY-2", amount: -10.00d},
        {paymentId: "PAY-3", amount: 75.00d}
    ];

    foreach Payment payment in payments {
        check producer->send({queueName: PAYMENTS_QUEUE}, {
            payload: payment,
            deliveryMode: solace:PERSISTENT
        });
        log:printInfo("Payment submitted", paymentId = payment.paymentId, amount = payment.amount);
    }

    check producer->close();
}
