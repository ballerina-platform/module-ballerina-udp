import ballerina/udp;

service on  new udp:Listener(9000) {

   remote function onDatagram() returns readonly & udp:Datagram? {

   }

   remote function onError() {

   }
}
