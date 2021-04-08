import ballerina/udp as u;

service on new u:Listener(8000) {

    remote function onBytes(byte[] & readonly data, u:Caller caller, u:Caller two) {

    }

    remote function onError(u:Error err, float b) {

    }
}
