import ballerinax/solace;

listener solace:Listener solaceListener = check new ("tcp://localhost:55555");

service on solaceListener {
    remote function onMessage(solace:Message message) returns error? {}
}

@solace:ServiceConfig {queueName: "orders"}
service on solaceListener {
    remote function onMessage(solace:Message message) returns error? {}
}
