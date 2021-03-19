import ballerina/udp;

service on  new udp:Listener(8000) {

    remote function onDatagram(readonly & udp:Datagram datagram) returns readonly & udp:Datagram | udp:Error? {
        return datagram;
    }

    remote function onBytes(readonly & byte[] data) returns readonly & byte[] | udp:Error? {
        return data;
    }
}
