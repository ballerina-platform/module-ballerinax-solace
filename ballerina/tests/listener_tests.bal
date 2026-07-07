// Copyright (c) 2026 WSO2 LLC. (http://www.wso2.org).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/lang.runtime;
import ballerina/test;

// ========================================
// Listener test destinations
// ========================================
const string LISTENER_AUTOACK_QUEUE = "test/listener/autoack/queue";
const string LISTENER_CLIENTACK_QUEUE = "test/listener/clientack/queue";
const string LISTENER_DIRECT_TOPIC = "test/listener/direct/topic";
const string LISTENER_NACK_QUEUE = "test/listener/nack/queue";
const string LISTENER_TX_COMMIT_QUEUE = "test/listener/tx/commit/queue";
const string LISTENER_TX_ROLLBACK_QUEUE = "test/listener/tx/rollback/queue";
const string LISTENER_DURABLE_TOPIC = "test/listener/durable/topic";
const string LISTENER_DURABLE_ENDPOINT = "test-listener-durable-endpoint";

// Polling step and max steps used to wait for asynchronous conditions (delivery, redelivery, etc.).
// Redelivery after a FAILED settlement outcome is usually immediate but can occasionally take
// tens of seconds on the broker, so the budget is deliberately generous; passing tests exit early.
const decimal POLL_STEP = 0.5;
const int POLL_MAX_STEPS = 180;

// ========================================
// Thread-safe recorder shared with services
// ========================================
isolated class Recorder {
    private string[] messages = [];
    private int errors = 0;
    private int attempts = 0;

    isolated function add(string message) {
        lock {
            self.messages.push(message);
        }
    }

    isolated function addError() {
        lock {
            self.errors += 1;
        }
    }

    isolated function nextAttempt() returns int {
        lock {
            self.attempts += 1;
            return self.attempts;
        }
    }

    isolated function attemptCount() returns int {
        lock {
            return self.attempts;
        }
    }

    isolated function count() returns int {
        lock {
            return self.messages.length();
        }
    }

    isolated function contains(string message) returns boolean {
        lock {
            return self.messages.indexOf(message) !is ();
        }
    }
}

// Polls until the recorder reports at least `minCount` messages, or the poll budget is exhausted.
isolated function waitForMessages(Recorder recorder, int minCount) {
    int step = 0;
    while step < POLL_MAX_STEPS && recorder.count() < minCount {
        runtime:sleep(POLL_STEP);
        step += 1;
    }
}

// Polls until the recorder reports at least `minAttempts` delivery attempts, or the poll budget is exhausted.
isolated function waitForAttempts(Recorder recorder, int minAttempts) {
    int step = 0;
    while step < POLL_MAX_STEPS && recorder.attemptCount() < minAttempts {
        runtime:sleep(POLL_STEP);
        step += 1;
    }
}

// Returns true if the given queue has no pending messages (i.e. all were acknowledged/committed).
isolated function queueIsEmpty(string queueName) returns boolean|error {
    MessageConsumer consumer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD},
        subscriptionConfig: {queueName, ackMode: CLIENT_ACK}
    });
    Message? leftover = check consumer->receiveNoWait();
    check consumer->close();
    return leftover is ();
}

isolated function connectionConfig() returns CommonConnectionConfiguration => {
    vpnName: MESSAGE_VPN,
    auth: {
        username: BROKER_USERNAME,
        password: BROKER_PASSWORD
    }
};

isolated function publish(Destination destination, string payload, DeliveryMode deliveryMode) returns error? {
    MessageProducer producer = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD}
    });
    check producer->send(destination, {payload: payload.toBytes(), deliveryMode});
    check producer->close();
}

// ========================================
// Services under test
// ========================================
final Recorder autoAckRecorder = new;

Service autoAckService = @ServiceConfig {
    queueName: LISTENER_AUTOACK_QUEUE,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(Message message) returns error? {
        autoAckRecorder.add(check string:fromBytes(message.payload));
    }
};

final Recorder directTopicRecorder = new;

Service directTopicService = @ServiceConfig {
    topicName: LISTENER_DIRECT_TOPIC,
    endpointType: DEFAULT
} service object {
    remote function onMessage(Message message) returns error? {
        directTopicRecorder.add(check string:fromBytes(message.payload));
    }
};

final Recorder clientAckRecorder = new;

Service clientAckService = @ServiceConfig {
    queueName: LISTENER_CLIENTACK_QUEUE,
    ackMode: CLIENT_ACK
} service object {
    remote function onMessage(Message message, Caller caller) returns error? {
        clientAckRecorder.add(check string:fromBytes(message.payload));
        check caller->ack(message);
    }
};

// ========================================
// Listener initialization tests
// ========================================
@test:Config {groups: ["listener", "init"]}
function testListenerInit() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.gracefulStop();
}

// ========================================
// Queue service with AUTO_ACK
// ========================================
@test:Config {groups: ["listener"]}
function testListenerQueueAutoAck() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(autoAckService);
    check solaceListener.'start();
    // Let the flow/subscription become fully active before publishing, so the first message is not
    // missed under load (a topic subscription that is not yet active does not capture the message).
    runtime:sleep(2);

    string payload = "listener-autoack-payload";
    check publish({queueName: LISTENER_AUTOACK_QUEUE}, payload, PERSISTENT);
    waitForMessages(autoAckRecorder, 1);
    boolean received = autoAckRecorder.contains(payload);
    // Give the auto-acknowledgement a moment to reach the broker before checking the queue.
    runtime:sleep(1);
    // Stop before asserting so a slow delivery does not leak the listener's session onto the broker.
    check solaceListener.gracefulStop();

    // AUTO_ACK must have settled the message: a fresh consumer should find the queue empty.
    boolean queueEmpty = check queueIsEmpty(LISTENER_AUTOACK_QUEUE);
    test:assertTrue(received, "Service should have received the published message");
    test:assertTrue(queueEmpty, "AUTO_ACK should have acknowledged the message (queue must be empty)");
}

// ========================================
// Direct topic service
// ========================================
@test:Config {groups: ["listener"]}
function testListenerDirectTopic() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(directTopicService);
    check solaceListener.'start();
    // Let the flow/subscription become fully active before publishing, so the first message is not
    // missed under load (a topic subscription that is not yet active does not capture the message).
    runtime:sleep(2);

    string payload = "listener-direct-topic-payload";
    check publish({topicName: LISTENER_DIRECT_TOPIC}, payload, DIRECT);
    waitForMessages(directTopicRecorder, 1);
    boolean received = directTopicRecorder.contains(payload);
    check solaceListener.gracefulStop();
    test:assertTrue(received, "Direct topic service should have received the message");
}

// ========================================
// Queue service with CLIENT_ACK via Caller
// ========================================
@test:Config {groups: ["listener", "clientack"]}
function testListenerClientAckWithCaller() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(clientAckService);
    check solaceListener.'start();
    // Let the flow/subscription become fully active before publishing, so the first message is not
    // missed under load (a topic subscription that is not yet active does not capture the message).
    runtime:sleep(2);

    string payload = "listener-clientack-payload";
    check publish({queueName: LISTENER_CLIENTACK_QUEUE}, payload, PERSISTENT);
    waitForMessages(clientAckRecorder, 1);
    boolean received = clientAckRecorder.contains(payload);
    // Give the acknowledgement a moment to reach the broker before checking the queue.
    runtime:sleep(1);
    // Stop before asserting so a slow delivery does not leak the listener's session onto the broker.
    check solaceListener.gracefulStop();

    // caller->ack must have settled the message.
    boolean queueEmpty = check queueIsEmpty(LISTENER_CLIENTACK_QUEUE);
    test:assertTrue(received, "Client-ack service should have received the message");
    test:assertTrue(queueEmpty, "caller->ack should have acknowledged the message (queue must be empty)");
}

// ========================================
// Negative: service without @ServiceConfig annotation
// ========================================
@test:Config {groups: ["listener", "negative"]}
function testListenerAttachWithoutAnnotation() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    Service noAnnotationService = service object {
        remote function onMessage(Message message) returns error? {
        }
    };
    error? result = solaceListener.attach(noAnnotationService);
    test:assertTrue(result is error, "Attaching a service without @ServiceConfig should fail");
    check solaceListener.gracefulStop();
}

// ========================================
// Negative: service missing the onMessage method
// ========================================
@test:Config {groups: ["listener", "negative"]}
function testListenerAttachWithoutOnMessage() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    Service noOnMessageService = @ServiceConfig {
        queueName: LISTENER_AUTOACK_QUEUE
    } service object {
        remote function onEvent(Message message) returns error? {
        }
    };
    error? result = solaceListener.attach(noOnMessageService);
    test:assertTrue(result is error, "Attaching a service without an 'onMessage' method should fail");
    check solaceListener.gracefulStop();
}

// ========================================
// Durable topic endpoint service
// ========================================
final Recorder durableTopicRecorder = new;

Service durableTopicService = @ServiceConfig {
    topicName: LISTENER_DURABLE_TOPIC,
    endpointType: DURABLE,
    endpointName: LISTENER_DURABLE_ENDPOINT,
    ackMode: AUTO_ACK
} service object {
    remote function onMessage(Message message) returns error? {
        durableTopicRecorder.add(check string:fromBytes(message.payload));
    }
};

@test:Config {groups: ["listener", "durable"]}
function testListenerDurableTopic() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(durableTopicService);
    check solaceListener.'start();
    // Let the flow/subscription become fully active before publishing, so the first message is not
    // missed under load (a topic subscription that is not yet active does not capture the message).
    runtime:sleep(2);

    string payload = "listener-durable-topic-payload";
    check publish({topicName: LISTENER_DURABLE_TOPIC}, payload, PERSISTENT);
    waitForMessages(durableTopicRecorder, 1);
    boolean received = durableTopicRecorder.contains(payload);
    check solaceListener.gracefulStop();
    test:assertTrue(received, "Durable topic endpoint service should have received the message");
}

// ========================================
// NACK with requeue -> redelivery
// ========================================
final Recorder nackRecorder = new;

Service nackService = @ServiceConfig {
    queueName: LISTENER_NACK_QUEUE,
    ackMode: CLIENT_ACK
} service object {
    remote function onMessage(Message message, Caller caller) returns error? {
        int attempt = nackRecorder.nextAttempt();
        nackRecorder.add(check string:fromBytes(message.payload));
        if attempt == 1 {
            // First delivery: negatively acknowledge with requeue so the broker redelivers.
            check caller->nack(message, requeue = true);
        } else {
            // Redelivery: acknowledge to settle the message.
            check caller->ack(message);
        }
    }
};

@test:Config {groups: ["listener", "clientack"]}
function testListenerNackRequeueRedelivery() returns error? {
    Listener solaceListener = check new (BROKER_URL, connectionConfig());
    check solaceListener.attach(nackService);
    check solaceListener.'start();
    // Let the flow/subscription become fully active before publishing, so the first message is not
    // missed under load (a topic subscription that is not yet active does not capture the message).
    runtime:sleep(2);

    string payload = "listener-nack-requeue-payload";
    check publish({queueName: LISTENER_NACK_QUEUE}, payload, PERSISTENT);
    waitForAttempts(nackRecorder, 2);
    boolean redelivered = nackRecorder.attemptCount() >= 2;
    check solaceListener.gracefulStop();

    boolean queueEmpty = check queueIsEmpty(LISTENER_NACK_QUEUE);
    test:assertTrue(redelivered, "NACK with requeue should cause the message to be redelivered");
    test:assertTrue(queueEmpty, "After redelivery and ack the queue must be empty");
}

// ========================================
// Transacted listener: commit
// ========================================
final Recorder txCommitRecorder = new;

Service txCommitService = @ServiceConfig {
    queueName: LISTENER_TX_COMMIT_QUEUE,
    ackMode: CLIENT_ACK
} service object {
    remote function onMessage(Message message, Caller caller) returns error? {
        txCommitRecorder.add(check string:fromBytes(message.payload));
        // Committing the transaction settles the consumed message.
        check caller->'commit();
    }
};

@test:Config {groups: ["listener", "transacted"]}
function testListenerTransactedCommit() returns error? {
    Listener solaceListener = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD}
    });
    check solaceListener.attach(txCommitService);
    check solaceListener.'start();
    // Let the flow/subscription become fully active before publishing, so the first message is not
    // missed under load (a topic subscription that is not yet active does not capture the message).
    runtime:sleep(2);

    string payload = "listener-tx-commit-payload";
    check publish({queueName: LISTENER_TX_COMMIT_QUEUE}, payload, PERSISTENT);
    waitForMessages(txCommitRecorder, 1);
    boolean received = txCommitRecorder.contains(payload);
    check solaceListener.gracefulStop();

    boolean queueEmpty = check queueIsEmpty(LISTENER_TX_COMMIT_QUEUE);
    test:assertTrue(received, "Transacted service should have received the message");
    test:assertTrue(queueEmpty, "After commit the queue must be empty");
}

// ========================================
// Transacted listener: rollback -> redelivery, then commit
// ========================================
final Recorder txRollbackRecorder = new;

Service txRollbackService = @ServiceConfig {
    queueName: LISTENER_TX_ROLLBACK_QUEUE,
    ackMode: CLIENT_ACK
} service object {
    remote function onMessage(Message message, Caller caller) returns error? {
        int attempt = txRollbackRecorder.nextAttempt();
        txRollbackRecorder.add(check string:fromBytes(message.payload));
        if attempt == 1 {
            // First delivery: roll back so the broker redelivers within the transaction.
            check caller->'rollback();
        } else {
            check caller->'commit();
        }
    }
};

@test:Config {groups: ["listener", "transacted"]}
function testListenerTransactedRollback() returns error? {
    Listener solaceListener = check new (BROKER_URL, {
        vpnName: MESSAGE_VPN,
        transacted: true,
        auth: {username: BROKER_USERNAME, password: BROKER_PASSWORD}
    });
    check solaceListener.attach(txRollbackService);
    check solaceListener.'start();
    // Let the flow/subscription become fully active before publishing, so the first message is not
    // missed under load (a topic subscription that is not yet active does not capture the message).
    runtime:sleep(2);

    string payload = "listener-tx-rollback-payload";
    check publish({queueName: LISTENER_TX_ROLLBACK_QUEUE}, payload, PERSISTENT);
    waitForAttempts(txRollbackRecorder, 2);
    boolean redelivered = txRollbackRecorder.attemptCount() >= 2;
    check solaceListener.gracefulStop();

    boolean queueEmpty = check queueIsEmpty(LISTENER_TX_ROLLBACK_QUEUE);
    test:assertTrue(redelivered, "Rollback should cause the message to be redelivered");
    test:assertTrue(queueEmpty, "After the committed redelivery the queue must be empty");
}
