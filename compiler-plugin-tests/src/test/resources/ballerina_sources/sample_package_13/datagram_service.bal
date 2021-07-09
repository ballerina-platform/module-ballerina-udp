import ballerina/udp;

service on new udp:Listener(9000) {

    remote function onDatagram(readonly & udp:Datagram datagram, udp:Caller caller, int a) returns udp:Datagram {
        return datagram;
    }
}
