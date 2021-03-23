import ballerina/udp;

service on  new udp:Listener(9000) {

   remote function onDatagram(udp:Datagram datagram) returns udp:Datagram | udp:Error? {

   }
}
