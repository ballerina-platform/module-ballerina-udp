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
// Initialized the UDP client.
udp:ConnectClient socketClient = check new ("localhost", PORT, timeout = 1);
// Maintain a counter from 0 to 255, sized of a byte.
byte sendCounter = 0;
public function main() returns error? {
    // Add a new file to the given file location. In error cases,
    // an error is returned. The local file is provided as a stream of
    // `byte[]` in which `5` is the block size.
    stream<byte[], io:Error?> fileStream
        = check io:fileReadBlocksAsStream("../resources/test.txt", 5);
    var fileIterator = fileStream.iterator();
    // Read the first data block from the file.
    record {|byte[] value;|}|io:Error? token = fileIterator.next();
    // Iterate through the `fileStream` till either the end of file is reached
    // or till an error has occurred.
    while (token is (record {|byte[] value;|})) {
        // Increment the counter as file is read.
        sendCounter = <byte>((<int>sendCounter + 1) % 256);
        byte[][] byteArrays = token.toArray();
        // Prepare the `byte[]` suitable to be sent to the server.
        byte[] byteArray = check prepareSendByteArray(sendCounter, byteArrays);
        // Sends the prepared `byte[]` to the server.
        check socketClient->writeBytes(byteArray);
        // Then, read the response of the server which should be as same as the
        // `sendCounter` byte.
        readonly & byte[] response = check socketClient->readBytes();
        int retryCounter = 0;
        // Untill that response is received, wait for 10 seconds.
        while response[0] != sendCounter && retryCounter < 10 {
            rt:sleep(1);
            response = check socketClient->readBytes();
            retryCounter += 1;
        }
        if retryCounter == 10 {
            // If the response has not correctly received the program ends with
            // an error message.
            io:print("Server has not received the datagram.");
            break;
        }
        // Iteratively try to get the next data block or the `()`.
        token = fileIterator.next();
    }
    // At the end of the file `()` is returned by the `fileIterator`.
    // Then again, the `sendCounter` is incremented and do the final data
    // transfer to the server.
    sendCounter = <byte>((<int>sendCounter + 1) % 256);
    check socketClient->writeBytes([sendCounter, <byte>TERMINAL]);
    // Finally, both the `socketClient` and `fileStream` are closed.
    check socketClient->close();
    check fileStream.close();

}

// Prepare the `byte[]` suitable to be sent to the server.
// This array starts with the byte of the `sendCounter`.
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
