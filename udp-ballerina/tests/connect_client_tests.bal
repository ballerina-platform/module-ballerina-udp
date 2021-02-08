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

import ballerina/log;
import ballerina/test;
import ballerina/io;

@test:Config {
     dependsOn: [testContentReceive]
}
function testConnectClientEcho() {
    ConnectClient|Error? socketClient = new("localhost", 48829);
    if (socketClient is ConnectClient) {
         string msg = "Echo from connet client";

        var sendResult = socketClient->writeBytes(msg.toBytes());
        if (sendResult is ()) {
            log:print("Data was sent to the remote host.");
        } else {
            test:assertFail(msg = sendResult.message());
        }
        string readContent = readConnectClientContent(socketClient);
        test:assertEquals(readContent, msg, "Found unexpected output");
        checkpanic socketClient->close();
        
    } else if (socketClient is Error) {
        log:printError("Error initializing UDP Client", err = socketClient);
    }
}


@test:Config {
    dependsOn: [testConnectClientEcho]
}
isolated function testConnectClientReadTimeOut() {
    ConnectClient|Error? socketClient = new("localhost", 48830, localHost = "localhost", timeoutInMillis = 1000);
    if (socketClient is ConnectClient) {
        
        var result = socketClient->readBytes();
        if (result is byte[]) {
            test:assertFail(msg = "No UDP service running on localhost:45830, no result should be returned");
        } else {
            log:print(result.message());
        }

        checkpanic socketClient->close();
        
    } else if (socketClient is Error) {
        log:printError("Error initializing UDP Client", err = socketClient);
    }
}

function readConnectClientContent(ConnectClient socketClient) returns string {
    string returnStr = "";
    var result = socketClient->readBytes();
    if (result is byte[]) {
        var str = getString(result, 50);
        if (str is string) {
            returnStr = <@untainted>str;
            io:println("Response is :", returnStr);
        } else {
            test:assertFail(msg = str.message());
        }
    } else {
        test:assertFail(msg = "Failed to receive the data");
    }
    return returnStr;
}
