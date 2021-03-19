import ballerina/udp as u;

service on  new u:Listener(8000) {

   remote function onBytes(byte[] & readonly data) returns readonly & byte[] | u:Error? {
        return data;
   }
}
