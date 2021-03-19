import ballerina/udp;

service on  new udp:Listener(9000) {

   remote function onDatagram(float data) returns readonly & udp:Datagram | udp:Error? {

   }
}
