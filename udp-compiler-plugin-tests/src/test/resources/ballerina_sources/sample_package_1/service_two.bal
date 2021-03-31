import ballerina/udp;

listener udp:Listener echo =  new udp:Listener(8000);

service on echo {

    remote function onDatagram(readonly & udp:Datagram datagram) returns udp:Datagram | udp:Error? {
        return datagram;
    }

    remote function onBytes(readonly & byte[] data) returns byte[] | udp:Error? {
        return data;
    }
}
