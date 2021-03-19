import ballerina/udp;

service on  new udp:Listener(8000) {

   remote function onBytes(byte[] & readonly data) returns readonly & byte[] | udp:Error? {
        return data;
   }
}
