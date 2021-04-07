import ballerina/udp as u;

service on new u:Listener(8000) {

    remote function onBytes(byte[] & readonly data) {

    }

    remote function onInt() {

    }
}
