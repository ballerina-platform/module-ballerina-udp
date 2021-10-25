import ballerina/udp as u;

listener u:Listener 'listener = new u:Listener(8000);

service on 'listener {

    remote function onBytes(readonly & byte[] data) returns byte[]|u:Error? {
        return ();
    }

    remote function onError(int err) {

    }
}
