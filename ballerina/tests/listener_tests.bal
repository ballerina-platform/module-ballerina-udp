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
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
import ballerina/log;
import ballerina/test;
import ballerina/io;

@test:Config {dependsOn: [testContentReceive]}
function testListenerRead() returns error? {
    Client socketClient = check new (localHost = "localhost");

    string[] messages = ["Log message one", "Log message two", "Log message three"];

    foreach var msg in messages {
        Datagram datagram = prepareDatagram(msg, remotePort = PORT1);

        check socketClient->sendDatagram(datagram);
        log:printInfo("Datagram was sent to the remote host.");
    }

    return check socketClient->close();
}

@test:Config {dependsOn: [testListenerRead]}
function testCallerSendBytes() returns error? {
    Client socketClient = check new (localHost = "localhost");

    string[] messages = ["Log message one", "Log message two", "Log message three"];

    foreach var msg in messages {
        Datagram datagram = prepareDatagram(msg, remotePort = PORT2);

        check socketClient->sendDatagram(datagram);
        log:printInfo("Datagram was sent to the remote host.");

        readonly & Datagram response = check socketClient->receiveDatagram();
        // assert echo response
        test:assertEquals(string:fromBytes(response.data), msg, "Found unexpected output");
    }

    return check socketClient->close();
}

@test:Config {dependsOn: [testCallerSendBytes]}
function testCallerSendDatagram() returns error? {
    Client socketClient = check new (localHost = "localhost");

    string[] messages = ["hi", "who are you?", "other"];

    foreach var msg in messages {
        Datagram datagram = prepareDatagram(msg, remotePort = PORT3);

        check socketClient->sendDatagram(datagram);
        log:printInfo("Datagram was sent to the remote host.");

        readonly & Datagram response = check socketClient->receiveDatagram();
        // assert echo response
        if (QuestionBank.hasKey(msg)) {
            test:assertEquals(string:fromBytes(response.data), QuestionBank.get(msg), "Found unexpected output");
        } else {
            io:println(string:fromBytes(response.data));
        }
    }

    return check socketClient->close();
}

@test:Config {dependsOn: [testCallerSendDatagram]}
function testReturnDatagram() returns error? {
    Client socketClient = check new (localHost = "localhost");

    string msg = "Hello Ballerina echo";
    Datagram datagram = prepareDatagram(msg, remotePort = PORT4);

    check socketClient->sendDatagram(datagram);
    log:printInfo("Datagram was sent to the remote host.");

    readonly & Datagram response = check socketClient->receiveDatagram();
    test:assertEquals(string:fromBytes(response.data), msg, "Found unexpected output");

    return check socketClient->close();
}

@test:Config {dependsOn: [testReturnDatagram]}
function testConnectedListener() returns error? {
    ConnectClient socketClient = check new ("localhost", PORT5, timeout = 1);

    string msg = "Echo from connet client";
    check socketClient->writeBytes(msg.toBytes());
    log:printInfo("Data was sent to the remote host.");

    (readonly & byte[])|Error res = socketClient->readBytes();
    if (res is (readonly & byte[])) {
        test:assertEquals(res, "You are running on 9999".toBytes(), "Found unexpected output");
    } else {
        // since the connected listener only accept the messages from the client running on port 9999
        // this read will result in a timeout error
        io:println("This client is not running on port 9999");
    }
    return check socketClient->close();
}

@test:Config {dependsOn: [testConnectedListener]}
function testListenerForSendingMultipleDatagrams() returns error? {
    Client socketClient = check new (timeout = 0.1);

    string msg = "Send me the data";

    _ = check socketClient->sendDatagram({
        data: msg.toBytes(),
        remoteHost: "localhost",
        remotePort: PORT6
    });

    int noOfBytesReceived = 0;
    readonly & Datagram|Error res = socketClient->receiveDatagram();

    while (res is (readonly & Datagram)) {
        noOfBytesReceived += res.data.length();
        res = socketClient->receiveDatagram();
    }

    // listener sending multiple datagrams this client should recive atleast one datagram
    if (noOfBytesReceived > 0) {
        io:println("Total number of bytes from received datagrams: ", noOfBytesReceived);
    } else {
        test:assertFail(msg = "No datagrams received by the client");
    }
    return check socketClient->close();
}

@test:Config {dependsOn: [testListenerForSendingMultipleDatagrams]}
function testListenerAttachDetatch() returns error? {
    Service dummyService = service object {
        remote function onBytes(readonly & byte[] data) returns Error? {
        }

        remote function onError(Error err) {
        }
    };
    check logServer.attach(dummyService);
    check logServer.detach(dummyService);
}

@test:Config {}
function testReadOnly() returns error? {
    ConnectClient socketClient = check new ("localhost", 9005);
    string msg = "Echo from connect client";
    check socketClient->writeBytes(msg.toBytes());
    readonly & byte[] response = check socketClient->readBytes();
    test:assertEquals(string:fromBytes(response), "true");
    return check socketClient->close();
}
