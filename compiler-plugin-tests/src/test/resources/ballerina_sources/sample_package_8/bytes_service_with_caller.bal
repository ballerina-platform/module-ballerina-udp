import ballerina/udp;

service on new udp:Listener(8100) {

    remote function onBytes(udp:Caller caller, byte[] & readonly data) returns udp:Error? {
        return check caller->sendBytes(data);
    }
}
