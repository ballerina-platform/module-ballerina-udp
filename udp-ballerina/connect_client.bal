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
// under the License.import ballerina/java;

import ballerina/java;

# Initializes the UDP connection oriented client based on the 
# provided configurations.
public client class ConnectClient {
   
    # Initializes the UDP connect client based on the 
    # provided configurations.
    # ```ballerina
    # udp:ConnectClient|udp:Error? socketClient = new("www.remote.com", 80,
    #                              localHost = "localHost");
    # ```
    # + remoteHost - The hostname of the remote host
    # + remotePort - The port number of the remmote host
    # + config - Connection oriented client related configuration
    public isolated function init(string remoteHost, int remotePort, *ConnectClientConfig config) returns Error? {
        return initConnectClient(self, remoteHost, remotePort, config);
    }

    # Sends the given data to the connected remote host.
    # ```ballerina
    # udp:Error? result = socketClient->writeBytes("msg".toBytes());
    # ```
    #
    # + data - The data need to be sent to the connected remote host
    # + return - () or else a `udp:Error` if the given data can't be sent
    isolated remote function writeBytes(byte[] data) returns Error? {
        return externConnectClientWrite(self, data);
    }

    # Reads data only from the connected remote host. 
    # ```ballerina
    # udp:Datagram|udp:Error result = socketClient->receiveDatagram();
    # ```
    #
    # + return - The byte[], or else a `udp:Error` if the data
    #            can't be read from the remote host
    isolated remote function readBytes() returns byte[]|Error {
        return externConnectClientRead(self);
    }

    # Free up the occupied socket 
    # ```ballerina
    # udp:Error? closeResult = socketClient->close();
    # ```
    #
    # + return - A `udp:Error` if it can't close the connection or else `()`
    isolated remote function close() returns Error? {
        return externConnectClientClose(self);
    }
}

# Configurations for the connection oriented udp client
# 
# + localHost - Local binding of the interface
# + timeoutInMillis - The socket reading timeout value to be used 
#                     in milliseconds. If this is not set,the default value
#                     of 300000 milliseconds (5 minutes) will be used.
public type ConnectClientConfig record {
   int timeoutInMillis = 30000;
   string? localHost = ();
   // can have other socket options
};


isolated function initConnectClient(ConnectClient connectClient, string remoteHost, int remotePort, ConnectClientConfig config) returns Error? =
@java:Method {
    name: "initEndpoint",
    'class: "org.ballerinalang.stdlib.udp.endpoint.ConnectClientActions"
} external;

isolated function externConnectClientClose(ConnectClient connectClient) returns Error? =
@java:Method {
    name: "close",
    'class: "org.ballerinalang.stdlib.udp.endpoint.ConnectClientActions"
} external;

isolated function externConnectClientRead(ConnectClient connectClient) returns byte[]|Error =
@java:Method {
    name: "read",
    'class: "org.ballerinalang.stdlib.udp.endpoint.ConnectClientActions"
} external;

isolated function externConnectClientWrite(ConnectClient connectClient, byte[] data) returns Error? =
@java:Method {
    name: "write",
    'class: "org.ballerinalang.stdlib.udp.endpoint.ConnectClientActions"
} external;
