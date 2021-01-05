// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/java;
import ballerina/log;
import ballerina/test;
import ballerina/io;

@test:BeforeSuite
function setup() {
    var result = startUdpServer();
}

@test:Config {
}
function testClientEcho() {
    Client|Error? socketClient = new({localHost:"localhost",timeoutInMillis:3000});
    if (socketClient is Client) {
         string msg = "Hello Ballerina echo";
        Datagram datagram = prepareDatagram(msg);

        var sendResult = socketClient->send(datagram);
        if (sendResult is ()) {
            log:print("Datagram was sent to the remote host.");
        } else {
            test:assertFail(msg = sendResult.message());
        }
        string readContent = receiveClientContent(socketClient);
        test:assertEquals(readContent, msg, "Found unexpected output");
        checkpanic socketClient->close();
        
    } else if (socketClient is Error) {
        log:printError("Error initializing UDP Client", err = socketClient);
    }
}

function getString(byte[] content, int numberOfBytes) returns @tainted string|io:Error {
    io:ReadableByteChannel byteChannel = check io:createReadableChannel(content);
    io:ReadableCharacterChannel characterChannel = new io:ReadableCharacterChannel(byteChannel, "UTF-8");
    return check characterChannel.read(numberOfBytes);
}

@test:Config {
    dependsOn: ["testClientEcho"]
}
function testContentReceive() {
    Client|Error? socketClient = new({localHost:"localhost",timeoutInMillis:3000});
     if (socketClient is Client) {
        string msg = "Hello server! send me the data";
         Datagram datagram = prepareDatagram(msg);

        var sendResult = socketClient->send(datagram);
              if (sendResult is ()) {
            log:print("Datagram was sent to the remote host.");
        } else {
            test:assertFail(msg = sendResult.message());
        }
        
        string readContent = receiveClientContent(socketClient);
        string expectedResponse = "Hi client! here is your data";
        test:assertEquals(readContent, expectedResponse, "Found an unexpected output");
        checkpanic socketClient->close();

    } else if (socketClient is Error) {
        log:printError("Error initializing UDP Client", err = socketClient);
    }
}

@test:AfterSuite{}
function stopAll() {
    var result = stopUdpServer();
}

isolated function prepareDatagram(string msg) returns Datagram {
    byte[] data =  msg.toBytes();
    return { data:<byte[] & readonly>data.cloneReadOnly(), remoteHost: "localhost", remotePort : 48829 };
}

function receiveClientContent(Client socketClient) returns string {
    string returnStr = "";
    var result = socketClient->receive();
    if (result is Datagram) {
        var str = getString(result.data, 50);
        if (str is string) {
            returnStr = <@untainted>str;
            io:println("Response is :", returnStr);
        } else {
            test:assertFail(msg = str.message());
        }
    } else {
        test:assertFail(msg = "failed");
    }
    return returnStr;
}

public function startUdpServer() returns Error? = @java:Method {
    'class: "org/ballerinalang/stdlib/udp/testutils/MockServerUtils"
} external;

public function stopUdpServer() returns Error? = @java:Method {
    'class: "org/ballerinalang/stdlib/udp/testutils/MockServerUtils"
} external;

public function passUdpContent(string content, int port) returns Error? = @java:Method {
    'class: "org/ballerinalang/stdlib/udp/testutils/MockServerUtils"
} external;
