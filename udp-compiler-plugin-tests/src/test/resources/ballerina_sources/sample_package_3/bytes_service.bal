import ballerina/udp;

service on  new udp:Listener(8000) {

    function onBytes(readonly & byte[] data) returns readonly & byte[] {
        return data;
    }

    function onError(udp:Error err) {

    }
}
