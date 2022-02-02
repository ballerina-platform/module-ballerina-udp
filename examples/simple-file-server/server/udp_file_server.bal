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
import ballerina/udp;

// Port which the server is to be listening.
const int PORT = 20211;
// Take the value of the terminal byte as 14.
const int TERMINAL = 14;

isolated service on new udp:Listener(PORT) {

    // Start the `sequenceNo` from zero as no data is received.
    private int sequenceNo = 0;
    // Open a byte channel to a file to append with receiving data.
    private io:WritableByteChannel byteChannel
        = checkpanic io:openWritableFile("dest.txt", option = io:APPEND);

    // This function is called when data is received.
    // Return the `sequenceNo` byte when successfully received.
    remote function onBytes(readonly & byte[] data) returns byte[]|udp:Error? {
        lock {
            // Initialize the flag for "End of File Reached".
            boolean eofReached = false;
            byte[] newData = [];
            // Copy data to be edited.
            newData = data.clone();
            // First byte is relevant to the `sequenceNo`.
            self.sequenceNo = newData[0];
            // Remove the `sequenceNo` byte to prepare the data to be written.
            _ = newData.remove(0);
            // Check the existance of the terminal byte.
            if newData[newData.length() - 1] == TERMINAL {
                // Set the `End of File Reached` flag to be used in the future.
                eofReached = true;
                // Remove the terminal byte to prepare the data to be written.
                _ = newData.remove(newData.length() - 1);
            }
            // Append the file with the received data.
            int|io:Error? result = self.byteChannel.write(newData, 0);
            if result is int {
                // Respond with the send counter of the latest written
                // `byte[]`.
                return [<byte>self.sequenceNo];
            } else if result is io:Error {
                return <udp:Error?>result;
            }
            if eofReached {
                // If the `End of File Reached` flag is set close the file.
                return <udp:Error?>(self.byteChannel).close();
            }
            return;
        }
    }

}
