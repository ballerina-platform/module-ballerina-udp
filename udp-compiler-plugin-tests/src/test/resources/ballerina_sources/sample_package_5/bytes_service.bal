import ballerina/udp;

service on  new udp:Listener(8000) {

   remote function onBytes(int data) returns readonly & byte[] | udp:Error? {

   }
}
