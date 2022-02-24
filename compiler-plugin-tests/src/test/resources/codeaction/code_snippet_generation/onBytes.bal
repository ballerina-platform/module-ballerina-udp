import ballerina/udp;

service on new udp:Listener(9000) {
	remote function onBytes(readonly & byte[] data) returns byte[]|udp:Error? {

	}
}
