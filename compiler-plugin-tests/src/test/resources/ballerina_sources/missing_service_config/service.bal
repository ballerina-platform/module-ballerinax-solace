import ballerinax/solace;

solace:Service invalidService = service object {
    remote function onMessage(solace:Message message) returns error? {
    }
};
