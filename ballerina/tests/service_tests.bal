// Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org).
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

listener Listener solaceListener = check new Listener(BROKER_URL, {
    messageVpn: MESSAGE_VPN,
    enableDynamicDurables: true,
    directOptimized: false,
    directTransport: false,
    auth: {
        username: BROKER_USERNAME,
        password: BROKER_PASSWORD
    }
});

isolated int queueServiceReceivedMessageCount = 0;
isolated int topicServiceReceivedMessageCount = 0;

final MessageProducer queueProducer = check new (BROKER_URL, {
    destination: {queueName: "service-test-queue"},
    messageVpn: MESSAGE_VPN,
    enableDynamicDurables: true,
    auth: {
        username: BROKER_USERNAME,
        password: BROKER_PASSWORD
    }
});
final MessageProducer topicProducer = check new (BROKER_URL, {
    destination: {topicName: "service-test-topic"},
    messageVpn: MESSAGE_VPN,
    enableDynamicDurables: true,
    auth: {
        username: BROKER_USERNAME,
        password: BROKER_PASSWORD
    }
});

@test:Config {
    groups: ["service"]
}
isolated function testQueueService() returns error? {
    Service consumerSvc = @ServiceConfig {
        queueName: "service-test-queue",
        pollingInterval: 2,
        receiveTimeout: 1
    } service object {
        remote function onMessage(Message message) returns error? {
            lock {
                queueServiceReceivedMessageCount += 1;
            }
        }
    };
    check solaceListener.attach(consumerSvc, "test-queue-service");
    check queueProducer->send({
        payload: "Hello World from queue".toBytes()
    });
    runtime:sleep(2);
    lock {
        test:assertEquals(queueServiceReceivedMessageCount, 1, "'service-test-queue' did not received the expected number of messages");
    }
}

@test:Config {
    groups: ["service"]
}
isolated function testTopicService() returns error? {
    Service consumerSvc = @ServiceConfig {
        topicName: "service-test-topic",
        subscriberName: "sub-1",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(Message message) returns error? {
            lock {
                topicServiceReceivedMessageCount += 1;
            }
        }
    };
    check solaceListener.attach(consumerSvc, "test-topic-service");
    check topicProducer->send({
        payload: "Hello World from topic".toBytes()
    });
    runtime:sleep(2);
    lock {
        test:assertEquals(topicServiceReceivedMessageCount, 1, "'service-test-topic' did not received the expected number of messages");
    }
}

isolated int serviceWithCallerReceivedMsgCount = 0;

@test:Config {
    groups: ["service"]
}
isolated function testServiceWithCaller() returns error? {
    Service consumerSvc = @ServiceConfig {
        sessionAckMode: CLIENT_ACKNOWLEDGE,
        topicName: "service-test-topic",
        subscriberName: "test.subscription",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        remote function onMessage(Message message, Caller caller) returns error? {
            lock {
                serviceWithCallerReceivedMsgCount += 1;
            }
            check caller->acknowledge(message);
        }
    };
    check solaceListener.attach(consumerSvc, "test-caller-svc");
    check topicProducer->send({
        payload: "Hello World from topic".toBytes()
    });
    runtime:sleep(2);
    lock {
        test:assertEquals(serviceWithCallerReceivedMsgCount, 1, "'service-test-topic' did not received the expected number of messages");
    }
}

isolated int ServiceWithTransactionsMsgCount = 0;

@test:Config {
    groups: ["service", "transactions"]
}
isolated function testServiceWithTransactions() returns error? {
    Service consumerSvc = @ServiceConfig {
        sessionAckMode: SESSION_TRANSACTED,
        topicName: "trx-service-test-topic",
        subscriberName: "test.transated.sub",
        pollingInterval: 1,
        receiveTimeout: 1
    } service object {
        isolated remote function onMessage(Message message, Caller caller) returns error? {
            lock {
                ServiceWithTransactionsMsgCount += 1;
            }
            var payload = message.payload;
            if payload !is byte[] {
                return;
            }
            string content = check string:fromBytes(payload);
            if content == "End of messages" {
                check caller->'commit();
            }
        }
    };
    check solaceListener.attach(consumerSvc, "test-transacted-service");

    MessageProducer topicProducer = check new (BROKER_URL, {
        destination: {topicName: "trx-service-test-topic"},
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });

    check topicProducer->send({
        payload: "This is the first message".toBytes()
    });
    check topicProducer->send({
        payload: "This is the second message".toBytes()
    });
    check topicProducer->send({
        payload: "This is the third message".toBytes()
    });
    check topicProducer->send({
        payload: "End of messages".toBytes()
    });
    runtime:sleep(5);
    lock {
        test:assertEquals(ServiceWithTransactionsMsgCount, 4, "Invalid number of received messages");
    }
}

@test:Config {
    groups: ["service"]
}
isolated function testServiceWithOnError() returns error? {
    Service consumerSvc = @ServiceConfig {
        sessionAckMode: CLIENT_ACKNOWLEDGE,
        topicName: "svc-test-topic"
    } service object {
        remote function onMessage(Message message) returns error? {
        }

        remote function onError(Error err) returns error? {
        }
    };
    check solaceListener.attach(consumerSvc, "test-onerror-service");
}

@test:Config {
    groups: ["service"]
}
isolated function testServiceReturningError() returns error? {
    Service consumerSvc = @ServiceConfig {
        sessionAckMode: CLIENT_ACKNOWLEDGE,
        topicName: "svc-test-topic"
    } service object {
        remote function onMessage(Message message) returns error? {
            return error("Error occurred while processing the message");
        }
    };
    check solaceListener.attach(consumerSvc, "test-onMessage-error-service");

    check topicProducer->send({
        payload: "This is a sample message".toBytes()
    });
    runtime:sleep(2);
}

@test:Config {
    groups: ["service"]
}
isolated function testListenerImmediateStop() returns error? {
    Listener msgListener = check new Listener(BROKER_URL, {
        messageVpn: MESSAGE_VPN,
        enableDynamicDurables: true,
        auth: {
            username: BROKER_USERNAME,
            password: BROKER_PASSWORD
        }
    });
    Service consumerSvc = @ServiceConfig {
        sessionAckMode: CLIENT_ACKNOWLEDGE,
        topicName: "svc-test-topic"
    } service object {
        remote function onMessage(Message message, Caller caller) returns error? {
        }
    };
    check msgListener.attach(consumerSvc, "consumer-svc");
    check msgListener.'start();
    runtime:sleep(2);
    check msgListener.immediateStop();
}

@test:Config {
    groups: ["service"]
}
isolated function testServiceAttachWithoutSvcPath() returns error? {
    Service consumerSvc = @ServiceConfig {
        sessionAckMode: CLIENT_ACKNOWLEDGE,
        topicName: "svc-test-topic"
    } service object {
        remote function onMessage(Message message, Caller caller) returns error? {
        }
    };
    check solaceListener.attach(consumerSvc);
}

@test:Config {
    groups: ["service"]
}
isolated function testServiceDetach() returns error? {
    Service consumerSvc = @ServiceConfig {
        sessionAckMode: CLIENT_ACKNOWLEDGE,
        topicName: "svc-test-topic"
    } service object {
        remote function onMessage(Message message, Caller caller) returns error? {
        }
    };
    check solaceListener.attach(consumerSvc, "consumer-svc");
    runtime:sleep(2);
    check solaceListener.detach(consumerSvc);
}
