import ballerina/udp;

service on new udp:Listener(9000) {

    remote function onDatagram(readonly & udp:Datagram datagram) returns readonly & udp:Datagram {
        return datagram;
    }

    remote function onError(udp:Error err) returns int|float? {
        return ();
    }
}
