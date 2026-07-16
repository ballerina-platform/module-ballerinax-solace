import ballerinax/solace;

solace:Service missingAnnotation = service object {
    remote function onMessage(solace:Message message) returns error? {}
};

solace:Service resourceMethod = @solace:ServiceConfig {queueName: "q"} service object {
    resource function get status() returns string => "ok";
    remote function onMessage(solace:Message message) returns error? {}
};

solace:Service missingOnMessage = @solace:ServiceConfig {queueName: "q"} service object {
};

solace:Service unsupportedMethod = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onEvent(solace:Message message) returns error? {}
};

solace:Service invalidParameterCount = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage() returns error? {}
};

solace:Service invalidMessageType = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage(string payload) returns error? {}
};

solace:Service invalidCallerPosition = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage(solace:Message message, solace:Message second) returns error? {}
};

solace:Service invalidOnError = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage(solace:Message message) returns error? {}
    remote function onError(solace:Message err) returns error? {}
};

solace:Service invalidReturn = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage(solace:Message message) returns string => "invalid";
};

type StringMessage record {|
    *solace:Message;
    string payload;
|};

solace:Service validService = @solace:ServiceConfig {queueName: "q"} service object {
    remote function onMessage(StringMessage message, solace:Caller caller) returns solace:Error? {}
    remote function onError(solace:Error err) returns error? {}
};
