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

import ballerina/jballerina.java;
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
    Client|Error? socketClient = new;
    if (socketClient is Client) {
         string msg = "Hello Ballerina echo";
        Datagram datagram = prepareDatagram(msg);

        var sendResult = socketClient->sendDatagram(datagram);
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

function getString(byte[] content, int numberOfBytes = 50) returns @tainted string|io:Error {
    io:ReadableByteChannel byteChannel = check io:createReadableChannel(content);
    io:ReadableCharacterChannel characterChannel = new io:ReadableCharacterChannel(byteChannel, "UTF-8");
    return check characterChannel.read(numberOfBytes);
}

@test:Config {
    dependsOn: [testClientEcho]
}
function testContentReceive() {
    Client|Error? socketClient = new(localHost = "localhost", timeoutInMillis = 3000);
     if (socketClient is Client) {
        string msg = "Hello server! send me the data";
         Datagram datagram = prepareDatagram(msg);

        var sendResult = socketClient->sendDatagram(datagram);
              if (sendResult is ()) {
            log:print("Datagram was sent to the remote host.");
        } else {
            test:assertFail(msg = sendResult.message());
        }
        
        string readContent = receiveClientContent(socketClient);
        string expectedResponse = "Hi client! here is your data";
        test:assertEquals(readContent, expectedResponse, "Found an unexpected output");

        // repeating the send and receive
        sendResult = socketClient->sendDatagram(datagram);
        if (sendResult is ()) {
            log:print("Datagram was sent to the remote host.");
        } else {
            test:assertFail(msg = sendResult.message());
        }
        
        readContent = receiveClientContent(socketClient);
        test:assertEquals(readContent, expectedResponse, "Found an unexpected output");

        checkpanic socketClient->close();

    } else if (socketClient is Error) {
        log:printError("Error initializing UDP Client", err = socketClient);
    }
}

@test:Config {
    dependsOn: [testContentReceive]
}
function testSendAndReciveLargeDataViaDatagram() returns error? {
    Client socketClient = check new(localHost = "localhost", timeoutInMillis = 3000);

    byte[] data = [];    
    data[8191] = <byte>97;
    data[16383] = <byte>98;
    data[24575] = <byte>99;
    check socketClient->sendDatagram({data: data, remoteHost : "localhost", remotePort : PORT4});
    io:println("Datagram sent with large data.");

    int expectedResponseArrayLengthOfFirstDatagramPacket = 8192;
    readonly & Datagram response = check socketClient->receiveDatagram();
    int receivedResponseArrayLenght = response.data.length();
    io:println("***********************: ", receivedResponseArrayLenght);
    test:assertTrue(receivedResponseArrayLenght == expectedResponseArrayLengthOfFirstDatagramPacket,
        msg = "Datagrams not recived properly");

    check socketClient->close();
}

@test:AfterSuite{}
function stopAll() {
    var result = stopUdpServer();
}

isolated function prepareDatagram(string msg, string remoteHost = "localhost", int remotePort = 48829) returns Datagram {
    byte[] data =  msg.toBytes();
    return { data, remoteHost: remoteHost, remotePort: remotePort };
}

function receiveClientContent(Client socketClient) returns string {
    string returnStr = "";
    var result = socketClient->receiveDatagram();
    if (result is (readonly & Datagram)) {
        var str = getString(result.data);
        if (str is string) {
            returnStr = <@untainted>str;
            io:println("Response is :", returnStr);
        } else {
            test:assertFail(msg = str.message());
        }
    } else {
        test:assertFail(msg = "Failed to receive the datagram");
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
