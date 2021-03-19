import ballerina/udp;

service on  new udp:Listener(8000) {

   remote function onBytes() returns readonly & byte[]? {

   }
}
