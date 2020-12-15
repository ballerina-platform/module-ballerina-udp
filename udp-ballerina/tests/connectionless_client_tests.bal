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
    Client socketClient = new ("localhost", 2000);
    string msg = "Hello Ballerina echo";
    Datagram datagram = {
        remoteAddress : {
            host : "localhost",
            port : 48829
        },
        data : msg.toBytes()
    };

    var sendResult = socketClient->send(datagram);
    if (sendResult is ()) {
        log:print("datagram sent to remote address");
    } else {
        test:assertFail(msg = sendResult.message());
    }
    string readContent = receiveClientContent(socketClient);
    test:assertEquals(readContent, msg, "Found unexpected output");
    checkpanic socketClient->close();
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
    Client socketClient = new("localhost", 2000);
    string msg = "hello server! send me the data";
    Datagram datagram = {
        remoteAddress : {
            host : "localhost",
            port : 48829
        },
        data : msg.toBytes()
    };

    var sendResult = socketClient->send(datagram);

    string readContent = receiveClientContent(socketClient);
    string expectedResponse = "hi client! here is your data";
    test:assertEquals(readContent, expectedResponse, "Found unexpected output");
    checkpanic socketClient->close();
}

@test:AfterSuite{}
function stopAll() {
    var result = stopUdpServer();
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
        test:assertFail(msg = result.message());
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
