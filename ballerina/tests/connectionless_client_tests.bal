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
function setup() returns error? {
    check startUdpServer();
}

@test:Config {}
function testClientEcho() returns error? {
    Client socketClient = check new;
    string msg = "Hello Ballerina echo";
    Datagram datagram = prepareDatagram(msg);

    check socketClient->sendDatagram(datagram);
    log:printInfo("Datagram was sent to the remote host.");

    readonly & Datagram response = check socketClient->receiveDatagram();
    test:assertEquals(string:fromBytes(response.data), msg, "Found an unexpected output");

    return check socketClient->close();
}

@test:Config {dependsOn: [testClientEcho]}
isolated function testInvalidLocalHostInClient() {
    Client|Error? socketClient = new (localHost = "invalid", timeout = 1.5);
    if socketClient is Client {
        test:assertFail("Provided invalid value for localHost this should return an Error");
    } else if socketClient is Error {
        test:assertEquals("Error initializing UDP Client", socketClient.message());
    }
}

@test:Config {dependsOn: [testInvalidLocalHostInClient]}
function testContentReceive() returns error? {
    Client socketClient = check new (localHost = "localhost", timeout = 3);

    string msg = "Hello server! send me the data";
    Datagram datagram = prepareDatagram(msg);

    check socketClient->sendDatagram(datagram);
    log:printInfo("Datagram was sent to the remote host.");

    readonly & Datagram response = check socketClient->receiveDatagram();
    string expectedResponseString = "Hi client! here is your data";
    test:assertEquals(string:fromBytes(response.data), expectedResponseString, "Found an unexpected output");

    // repeating the send and receive
    check socketClient->sendDatagram(datagram);
    log:printInfo("Datagram was sent to the remote host.");

    response = check socketClient->receiveDatagram();
    test:assertEquals(string:fromBytes(response.data), expectedResponseString, "Found an unexpected output");

    return check socketClient->close();
}

@test:Config {dependsOn: [testContentReceive]}
function testSendAndReciveLargeDataViaDatagram() returns error? {
    Client socketClient = check new (localHost = "localhost", timeout = 3);

    byte[] data = [];
    data[8191] = <byte>97;
    data[16383] = <byte>98;
    data[24575] = <byte>99;
    check socketClient->sendDatagram({
        data: data,
        remoteHost: "localhost",
        remotePort: PORT4
    });
    io:println("Datagram sent with large data.");

    int expectedResponseArrayLengthOfFirstDatagramPacket = 2048;
    readonly & Datagram response = check socketClient->receiveDatagram();
    int receivedResponseArrayLenght = response.data.length();

    test:assertTrue(receivedResponseArrayLenght == expectedResponseArrayLengthOfFirstDatagramPacket, 
    "Datagrams not recived properly");

    return check socketClient->close();
}

@test:AfterSuite {}
function stopAll() returns error? {
    check stopUdpServer();
}

isolated function prepareDatagram(string msg, string remoteHost = "localhost", int remotePort = 48829) returns Datagram {
    byte[] data = msg.toBytes();
    return {
        data,
        remoteHost: remoteHost,
        remotePort: remotePort
    };
}

public function startUdpServer() returns error? = @java:Method 
{'class: "io.ballerina.stdlib.udp.testutils.MockServerUtils"} external;

public function stopUdpServer() returns error? = @java:Method 
{'class: "io.ballerina.stdlib.udp.testutils.MockServerUtils"} external;

public function passUdpContent(string content, int port) returns error? = @java:Method 
{'class: "io.ballerina.stdlib.udp.testutils.MockServerUtils"} external;
