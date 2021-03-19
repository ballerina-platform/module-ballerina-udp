import ballerina/udp;

service on  new udp:Listener(9000) {

   remote function onDatagram(readonly & udp:Datagram datagram, int caller) returns readonly & udp:Datagram | udp:Error? {

   }
}
