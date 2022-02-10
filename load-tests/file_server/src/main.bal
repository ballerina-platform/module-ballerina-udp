// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/http;
import ballerina/io;
import ballerina/lang.runtime;
import ballerina/time;
import ballerina/udp;

int errorCount = 0;
int sentCount = 0;
int receivedCount = 0;
int deletedCount = 0;
time:Utc startedTime = time:utcNow();
time:Utc endedTime = time:utcNow();
boolean finished = false;
const int PORT = 8080;
const int TERMINAL = 14;
service /udp on new http:Listener(9100) {

    resource function get publish() returns boolean {
        error? result = startListener();
        if result is error {
            return false;
        }
        errorCount = 0;
        sentCount = 0;
        startedTime = time:utcNow();
        endedTime = time:utcNow();
        finished = false;
        _ = start publishMessages();
        return true;
    }

    resource function get getResults() returns boolean|map<string> {
        if finished {
            return {
                errorCount: errorCount.toString(),
                time: time:utcDiffSeconds(endedTime, startedTime).toString(),
                sentCount: sentCount.toString(),
                receivedCount: receivedCount.toString()
            };
        }
        return false;
    }
}

public function publishMessages() returns error? {
    udp:ConnectClient socketClient = check new ("localhost", PORT, timeout = 1);
    startedTime = time:utcNow();
    byte sequenceNo = 0;
    time:Utc expiryTime = time:utcAddSeconds(startedTime, 200);
    while time:utcDiffSeconds(expiryTime, time:utcNow()) > 0D {
        stream<byte[], io:Error?> fileStream = check io:fileReadBlocksAsStream("resources/test.txt", 5);
        var fileIterator = fileStream.iterator();
        record {|byte[] value;|}|io:Error? fileChunk = fileIterator.next();
        while fileChunk is (record {|byte[] value;|}) {
            sequenceNo = <byte>((<int>sequenceNo + 1) % 256);
            byte[][] byteArrays = fileChunk.toArray();
            byte[] byteArray = check prepareSendByteArray(sequenceNo, byteArrays);
            udp:Error? result = socketClient->writeBytes(byteArray);
            if result is udp:Error {
                errorCount += 1;
            } else {
                sentCount += 1;
            }
            readonly & byte[] response = check socketClient->readBytes();
            receivedCount += 1;
            int waitCounter = 0;
            while response[0] != sequenceNo && waitCounter < 10 {
                runtime:sleep(1);
                readonly & byte[]|udp:Error resp = socketClient->readBytes();
                if resp is udp:Error {
                    errorCount += 1;
                } else {
                    response = resp;
                    waitCounter += 1;
                    receivedCount += 1;
                }
            }
            if waitCounter == 10 {
                errorCount += 1;
                break;
            }
            fileChunk = fileIterator.next();
        }
        sequenceNo = <byte>((<int>sequenceNo + 1) % 256);
        udp:Error? result = socketClient->writeBytes([sequenceNo, <byte>TERMINAL]);
        if (result is udp:Error) {
            errorCount += 1;
        } else {
            sentCount += 1;
        }
        check fileStream.close();
    }
    check socketClient->close();
    finished = true;
    endedTime = time:utcNow();
}

function prepareSendByteArray(byte counter, byte[][] bArrays) returns byte[]|error {
    byte[] result = [];
    result.push(counter);
    foreach byte[] bArray in bArrays {
        byte[] nonReadOnlyByteArray = (<byte[] & readonly>bArray).clone();
        foreach byte b in nonReadOnlyByteArray {
            result.push(b);
        }
    }
    return result;
}

function startListener() returns error? {
    udp:Listener udpListener = check new(8080);
    check udpListener.attach(udpServer);
    check udpListener.start();
    runtime:registerListener(udpListener);
}

udp:Service udpServer = service object {
    private int sequenceNo = 0;

    remote function onBytes(readonly & byte[] data) returns byte[]|udp:Error? {
        lock {
            byte[] newData = [];
            newData = data.clone();
            self.sequenceNo = newData[0];
            _ = newData.remove(0);
            if newData[newData.length() - 1] == TERMINAL {
                _ = newData.remove(newData.length() - 1);
            }
            return [<byte>self.sequenceNo];
        }
    }
};
