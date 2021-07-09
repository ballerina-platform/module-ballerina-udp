import ballerina/udp;

service on new udp:Listener(8000) {

    remote function onBytes(float caller, readonly & byte[] data) returns byte[]|udp:Error? {

    }
}
