import ballerina/udp;

service on new udp:Listener(9000) {

    remote function onDatagram() returns udp:Datagram? {

    }

    remote function onError() {

    }
}
