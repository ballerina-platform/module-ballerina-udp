import ballerina/udp;

service on new udp:Listener(8000) {

    remote function onBytes(int data) returns byte[]|udp:Error? {
        return ();
    }
}
