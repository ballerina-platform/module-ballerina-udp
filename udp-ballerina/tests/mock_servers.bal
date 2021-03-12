// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
import ballerina/io;

const int PORT1 = 9000;
const int PORT2 = 8080;
const int PORT3 = 9001;
const int PORT4 = 9002;
const int PORT5 = 9003;
const int PORT6 = 9004;

listener Listener logServer = new Listener(PORT1);
listener Listener echoServer = new Listener(PORT2);
listener Listener botServer = new Listener(PORT3);

service on logServer {

    remote function onBytes(readonly & byte[] data) returns Error? {
        io:println(string:fromBytes(data));
    }

    remote function onError(readonly & Error err) {
        io:println(err.message());
    }
}

service on echoServer {

    remote function onBytes(readonly & byte[] data) returns (readonly & byte[])|Error? {
        io:println("Received by listener:", string:fromBytes(data));
        return data;
    }

    remote function onError(readonly & Error err) {
        io:println(err);
    }
}

map<string> QuestionBank = {
    "hi": "hi there!",
    "who are you?": "I'm a ballerina bot"
};

service on botServer {

    remote function onDatagram(readonly & Datagram datagram, Caller caller) returns Datagram|Error? {
        string|error? dataString = string:fromBytes(datagram.data);
        io:println("Received data: ", dataString);
        if (dataString is string && QuestionBank.hasKey(dataString)) {
            string? response = QuestionBank[dataString];
            if (response is string) {
                return prepareDatagram(response, <string>caller.remoteHost, <int>caller.remotePort);
            }
        }
        Error? res = caller->sendDatagram(prepareDatagram("Sorry,I Can’t help you with that", <string>caller.remoteHost, <int>
        caller.remotePort));
    }

    remote function onError(readonly & Error err) {
        io:println(err);
    }
}

service on new Listener(PORT4) {
    remote function onDatagram(readonly & Datagram datagram, Caller caller) returns Datagram|Error? {
        io:println("Datagram received by listener datagram data lenght is: ", datagram.data.length());
        return datagram;
    }
}

// this listener only listen to the client running on localhost:9999
service on new Listener(PORT5, remoteHost = "localhost", remotePort = 9999) {
    remote function onBytes(readonly & byte[] data) returns (readonly & byte[])|Error? {
        io:println("Received by connected listener:", string:fromBytes(data));
        return <byte[] & readonly>("You are running on 9999".toBytes().cloneReadOnly());
    }
}

service on new Listener(PORT6) {
    remote function onDatagram(Caller caller, readonly & Datagram datagram) returns Error? {
        io:println("Datagram received by listener: ", string:fromBytes(datagram.data));
        // return large data this will break the data in to multiple datagram and send it to client
        byte[] response = [];
        response[80000] = 97;
        check caller->sendDatagram({
            data: response,
            remoteHost: <string>caller.remoteHost,
            remotePort: <int>caller.remotePort
        });
    }
}
