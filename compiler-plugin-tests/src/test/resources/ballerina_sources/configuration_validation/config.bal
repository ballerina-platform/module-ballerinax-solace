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
