import ballerinax/solace;

function acceptService(solace:Service solaceService) {
}

function passService() {
    acceptService(service object {
        remote function onMessage(solace:Message message) returns error? {}
    });
}
