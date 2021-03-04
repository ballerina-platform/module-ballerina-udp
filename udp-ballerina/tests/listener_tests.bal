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
// under the License.import ballerina/log;
import ballerina/log;
import ballerina/test;
import ballerina/io;

@test:Config {dependsOn: [testContentReceive]}
function testListenerRead() {
    Client|Error? socketClient = new (localHost = "localhost");
    if (socketClient is Client) {
        string[] messages = ["Log message one", "Log message two", "Log message three"];

        foreach var msg in messages {
            Datagram datagram = prepareDatagram(msg, remotePort = PORT1);

            var sendResult = socketClient->sendDatagram(datagram);
            if (sendResult is ()) {
                log:print("Datagram was sent to the remote host.");
            } else {
                test:assertFail(msg = sendResult.message());
            }
        }

        checkpanic socketClient->close();

    } else if (socketClient is Error) {
        log:printError("Error initializing UDP Client", err = socketClient);
    }
}

@test:Config {dependsOn: [testListenerRead]}
function testCallerSendBytes() {
    Client|Error? socketClient = new (localHost = "localhost");
    if (socketClient is Client) {
        string[] messages = ["Log message one", "Log message two", "Log message three"];

        foreach var msg in messages {
            Datagram datagram = prepareDatagram(msg, remotePort = PORT2);

            var sendResult = socketClient->sendDatagram(datagram);
            if (sendResult is ()) {
                log:print("Datagram was sent to the remote host.");
            } else {
                test:assertFail(msg = sendResult.message());
            }

            string readContent = receiveClientContent(socketClient);
            // assert echo response
            test:assertEquals(readContent, msg, "Found unexpected output");

        }

        checkpanic socketClient->close();

    } else if (socketClient is Error) {
        log:printError("Error initializing UDP Client", err = socketClient);
    }
}

@test:Config {dependsOn: [testCallerSendBytes]}
function testCallerSendDatagram() {
    Client|Error? socketClient = new (localHost = "localhost");
    if (socketClient is Client) {
        string[] messages = ["hi", "who are you?", "other"];

        foreach var msg in messages {
            Datagram datagram = prepareDatagram(msg, remotePort = PORT3);

            var sendResult = socketClient->sendDatagram(datagram);
            if (sendResult is ()) {
                log:print("Datagram was sent to the remote host.");
            } else {
                test:assertFail(msg = sendResult.message());
            }

            string response = receiveClientContent(socketClient);
            io:println("Response from botServer: ", response);

        }

        checkpanic socketClient->close();

    } else if (socketClient is Error) {
        log:printError("Error initializing UDP Client", err = socketClient);
    }
}

@test:Config {dependsOn: [testCallerSendDatagram]}
function testReturnDatagram() {
    Client|Error? socketClient = new (localHost = "localhost");
    if (socketClient is Client) {
        string msg = "Hello Ballerina echo";
        Datagram datagram = prepareDatagram(msg, remotePort = PORT4);

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

@test:Config {dependsOn: [testReturnDatagram]}
function testConnectedListener() returns error? {
    ConnectClient|Error? socketClient = new ("localhost", PORT5, timeout = 1);
    if (socketClient is ConnectClient) {
        string msg = "Echo from connet client";

        var sendResult = socketClient->writeBytes(msg.toBytes());
        if (sendResult is ()) {
            log:print("Data was sent to the remote host.");
        } else {
            test:assertFail(msg = sendResult.message());
        }
        (readonly & byte[])|Error res = socketClient->readBytes();
        if (res is (readonly & byte[])) {
            test:assertEquals(res, "You are running on 9999".toBytes(), "Found unexpected output");
        } else {
            // since the connected listener only accept the messages from the client running on port 9999
            // this read will result in a timeout error
            io:println("This client is not running on port 9999");
        }
        check socketClient->close();
    } else if (socketClient is Error) {
        log:printError("Error initializing UDP Client", err = socketClient);
    }
}

@test:Config {dependsOn: [testConnectedListener], enable:true}
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
    check socketClient->close();
}
