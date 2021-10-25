import ballerina/udp;

service on new udp:Listener(9100) {

    remote function onDatagram(readonly & udp:Datagram datagram, udp:Caller caller) returns udp:Error? {
        return check caller->sendDatagram(datagram);
    }

    remote function onError(udp:Error err) {

    }
}
