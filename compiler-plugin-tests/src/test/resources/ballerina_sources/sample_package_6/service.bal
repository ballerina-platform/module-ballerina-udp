import ballerina/udp;

service on new udp:Listener(8000) {

    remote function onBytes(readonly & byte[] data) returns byte[]|udp:Error? {
        return ();
    }

    remote function onError(int err) {

    }
}
