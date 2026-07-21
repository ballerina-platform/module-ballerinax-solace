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

import ballerinax/solace;

solace:QueueConfiguration defaultDurableQueue = {};
solace:QueueConfiguration durableQueue = {durability: solace:DURABLE};
solace:QueueConfiguration emptyDurableQueue = {queueName: ""};

solace:TopicConfiguration durableTopic = {topicName: "orders", durability: solace:DURABLE};
solace:TopicConfiguration emptyDurableTopic = {
    topicName: "orders",
    durability: solace:DURABLE,
    endpointName: ""
};

solace:Service durableTopicService = @solace:ServiceConfig {
    topicName: "orders",
    durability: solace:DURABLE
} service object {
    remote function onMessage(solace:Message message) returns error? {}
};

solace:QueueConfiguration temporaryQueue = {durability: solace:TEMPORARY};
solace:TopicConfiguration temporaryTopic = {topicName: "events"};
solace:QueueConfiguration namedQueue = {queueName: "orders"};
solace:TopicConfiguration namedDurableTopic = {
    topicName: "orders",
    durability: solace:DURABLE,
    endpointName: "orders-endpoint"
};

function dynamicQueue(solace:Durability durability, string queueName) returns solace:QueueConfiguration {
    return {durability, queueName};
}

function dynamicTopic(solace:Durability durability, string endpointName) returns solace:TopicConfiguration {
    return {topicName: "orders", durability, endpointName};
}
