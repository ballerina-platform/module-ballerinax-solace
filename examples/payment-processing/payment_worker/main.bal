import ballerina/log;
import ballerinax/solace;

configurable string brokerUrl = "tcp://localhost:55554";
configurable string vpnName = "default";
configurable string username = "admin";
configurable string password = "admin";

const string PAYMENTS_QUEUE = "payments";

// Simulates a transient downstream outage on this payment's first delivery only.
const string TRANSIENT_FAILURE_PAYMENT = "PAY-3";

type Payment record {|
    string paymentId;
    decimal amount;
|};

listener solace:Listener paymentListener = check new (brokerUrl, {
    vpnName,
    auth: {username, password}
});

@solace:ServiceConfig {
    queueName: PAYMENTS_QUEUE,
    ackMode: solace:CLIENT_ACK
}
service on paymentListener {

    remote function onMessage(record {|*solace:Message; Payment payload;|} message, solace:Caller caller)
            returns error? {
        Payment payment = message.payload;

        if payment.amount <= 0d {
            log:printWarn("Rejecting payment with invalid amount", paymentId = payment.paymentId,
                    amount = payment.amount);
            // requeue = false: the broker moves the message straight to the dead message queue
            // instead of redelivering it - retrying bad data would never make it valid.
            check caller->nack(message, requeue = false);
            return;
        }

        if payment.paymentId == TRANSIENT_FAILURE_PAYMENT && message.redelivered != true {
            log:printWarn("Simulating a transient downstream failure", paymentId = payment.paymentId);
            // requeue = true: the broker redelivers the message so it gets another attempt.
            check caller->nack(message, requeue = true);
            return;
        }

        log:printInfo("Payment processed", paymentId = payment.paymentId, amount = payment.amount,
                redelivered = message.redelivered ?: false);
        check caller->ack(message);
    }
}
