import ballerinax/solace;

type AliasedService solace:Service;

AliasedService missingObjectServiceConfig = service object {
    remote function onMessage(solace:Message message) returns error? {}
};

type AliasedListener solace:Listener;

listener AliasedListener aliasedListener = check new ("tcp://localhost:55555");

service on aliasedListener {
    remote function onMessage(solace:Message message) returns error? {}
}
