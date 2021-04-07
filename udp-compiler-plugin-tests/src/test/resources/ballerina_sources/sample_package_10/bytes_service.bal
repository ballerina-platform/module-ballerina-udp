import ballerina/udp as u;
import sample_10.module as m;

service on  new u:Listener(8000) {

   remote function onBytes(byte[] & readonly data) returns byte[] | u:Error? {
        return data;
   }
}

service on new m:Listener() {

}
