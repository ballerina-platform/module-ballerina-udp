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

import ballerina/test;

service on new Listener(8081) {

    remote function onDatagram(readonly & Datagram datagram) returns Datagram|Error? {
        byte[] dataArr = datagram.data;
        byte _ = dataArr.pop();
        return datagram;
    }
}

service on new Listener(8082) {

    remote function onDatagram(readonly & Datagram datagram) returns Datagram|Error? {
        return error Error("Error occurred");
    }
}

@test:Config {}
function testPanicFromRemoteServer() returns error? {
    ConnectClient socketClient = check new ("localhost", 8081);

    string msg = "panic from the service";
    check socketClient->writeBytes(msg.toBytes());

    return check socketClient->close();
}

@test:Config {}
function testErrorReturnFromRemoteServer() returns error? {
    ConnectClient socketClient = check new ("localhost", 8082);

    string msg = "error";
    check socketClient->writeBytes(msg.toBytes());

    return check socketClient->close();
}