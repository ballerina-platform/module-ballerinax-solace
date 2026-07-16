import ballerinax/solace;

solace:Service invalidOnMessageRest = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage(solace:Message message, string... extras) returns error? {}
};

solace:Service invalidOnErrorRest = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage(solace:Message message) returns error? {}
    remote function onError(solace:Error err, string... extras) returns error? {}
};
