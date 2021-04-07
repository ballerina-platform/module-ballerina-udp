import ballerina/udp;

service on new udp:Listener(9000) {

    remote function onDatagram(float data) returns udp:Datagram|udp:Error? {

    }
}
