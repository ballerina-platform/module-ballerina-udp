import ballerina/udp;
import sample_10.module;

service on new udp:Listener(9000) {

    remote function onDatagram(readonly & udp:Datagram datagram) returns udp:Datagram|udp:Error? {
        return datagram;
    }

    remote function onError(udp:Error err) {

    }
}

service on new module:Listener() {

}
