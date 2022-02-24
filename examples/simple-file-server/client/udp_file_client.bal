// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/io;
import ballerina/udp;
import ballerina/lang.runtime as rt;

// Port which the client is sending data.
const int PORT = 20211;
// Take the value of the terminal byte as 14.
const int TERMINAL = 14;

public function main() returns error? {
    // Initialized the UDP client.
    udp:ConnectClient socketClient = check new ("localhost", PORT, timeout = 1);
    // Maintain a counter from 0 to 255, sized of a byte.
    byte sequenceNo = 0;
    // The file is given as a stream of `byte[]` where `5` is the block size.
    stream<byte[], io:Error?> fileStream
        = check io:fileReadBlocksAsStream("../resources/test.txt", 5);
    var fileIterator = fileStream.iterator();
    // Read the first data block from the file.
    record {|byte[] value;|}|io:Error? fileChunk = fileIterator.next();
    // Iterate through the `fileStream` till either the end of file is reached.
    while fileChunk is record {|byte[] value;|} {
        // Increment the counter as file is read.
        sequenceNo = <byte>((<int>sequenceNo + 1) % 256);
        byte[][] byteArrays = fileChunk.toArray();
        // Prepare the `byte[]` suitable to be sent to the server.
        byte[] byteArray = check prepareSendByteArray(sequenceNo, byteArrays);
        // Sends the prepared `byte[]` to the server.
        check socketClient->writeBytes(byteArray);
        // Check the response with the `sequenceNo` byte.
        readonly & byte[] response = check socketClient->readBytes();
        int waitCounter = 0;
        // Until that response is received, wait for 10 seconds.
        while response[0] != sequenceNo && waitCounter < 10 {
            rt:sleep(1);
            response = check socketClient->readBytes();
            waitCounter += 1;
        }
        if waitCounter == 10 {
            // If the response has not correctly received the program ends with
            // an error message.
            io:print("Server has not received the datagram.");
            break;
        }
        // Iteratively try to get the next data block or the `()`.
        fileChunk = fileIterator.next();
    }
    // At the end of the file `()` is returned by the `fileIterator`.
    // Then again, the `sequenceNo` is incremented and do the final data
    // transfer to the server.
    sequenceNo = <byte>((<int>sequenceNo + 1) % 256);
    check socketClient->writeBytes([sequenceNo, <byte>TERMINAL]);
    // Finally, both the `socketClient` and `fileStream` are closed.
    check socketClient->close();
    check fileStream.close();

}

// Prepare the `byte[]` suitable to be sent to the server.
// This array starts with the byte of the `sequenceNo`.
// The remaining content is the file content.
// As the end of the file is reached file content is replaced by the
// terminal byte which has the value of 14.
function prepareSendByteArray(byte counter, byte[][] bArrays)
        returns byte[]|error {
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
