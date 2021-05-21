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
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/jballerina.java;

# Initializes the UDP connectionless client based on the provided configurations.
public isolated client class Client {
   
    # Initializes the UDP connectionless client based on the provided 
    # configurations.
    # ```ballerina
    # udp:Client|udp:Error? socketClient = new(localHost = "localhost");
    # ```
    #
    # + config - Connectionless client-related configuration
    # + return - `()` or else a `udp:Error` if the given data cannot be sent
    public isolated function init(*ClientConfiguration config) returns Error? {
        return initConnectionlessClient(self, config);
    }

    # Sends the given data to the specified remote host.
    # ```ballerina
    # udp:Error? result = socketClient->sendDatagram({remoteHost: "localhost",
    #            remotePort: 48826, data:"msg".toBytes()});
    # ```
    #
    # + datagram - Contains the data to be sent to the remote host
    #              and the address of the remote host
    # + return - `()` or else a `udp:Error` if the given data cannot be sent
    isolated remote function sendDatagram(Datagram datagram) returns Error? {
        return externConnectionlessSend(self, datagram);
    }

    # Reads data from the remote host. 
    # ```ballerina
    # udp:Datagram|udp:Error result = socketClient->receiveDatagram();
    # ```
    #
    # + return - A `udp:Datagram` or else a `udp:Error` if the data
    #            cannot be read from the remote host
    isolated remote function receiveDatagram() returns (readonly & Datagram)|Error {
        return externConnectionlessReceive(self);
    }

    # Free up the occupied socket.
    # ```ballerina
    # udp:Error? closeResult = socketClient->close();
    # ```
    #
    # + return - A `udp:Error` if it can't close the connection or else `()`
    isolated remote function close() returns Error? {
        return externConectionlessClientClose(self);
    }
}


# A self-contained, independent entity of data carrying sufficient information
# to be routed from the source to the destination nodes without reliance
# on earlier exchanges between the nodes and the transporting network.
# 
# + remoteHost - The hostname or the IP address of the remote host
# + remotePort - The port number of the remote host
# + data - The content which needs to be transported to the remote host
public type Datagram record {|
   string remoteHost;
   int remotePort;
   byte[] data;
|};

# Configurations for the connectionless UDP client.
# 
# + localHost - Local binding of the interface
# + timeout - The socket-reading timeout value to be used 
#             in seconds. If this is not set,the default value
#             of 300 seconds (5 minutes) will be used
public type ClientConfiguration record {
   decimal timeout = 300;
   string localHost?;
   // can have other socket options
};


isolated function initConnectionlessClient(Client udpClient, ClientConfiguration config) returns Error? =
@java:Method {
    name: "init",
    'class: "org.ballerinalang.stdlib.udp.nativeclient.Client"
} external;

isolated function externConectionlessClientClose(Client udpClient) returns Error? =
@java:Method {
    name: "close",
    'class: "org.ballerinalang.stdlib.udp.nativeclient.Client"
} external;

isolated function externConnectionlessReceive(Client udpClient) returns (readonly & Datagram)|Error =
@java:Method {
    name: "receive",
    'class: "org.ballerinalang.stdlib.udp.nativeclient.Client"
} external;

isolated function externConnectionlessSend(Client udpClient, Datagram datagram) returns Error? =
@java:Method {
    name: "send",
    'class: "org.ballerinalang.stdlib.udp.nativeclient.Client"
} external;
