import ballerina/udp;

service on new udp:Listener(8000) {

    remote function onDatagram(readonly & udp:Datagram datagram) returns udp:Datagram|error? {
        return datagram;
    }
}
