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
// under the License.import ballerina/java;

import ballerina/java;

# Initializes the UDP connectionless client based on the provided configurations.
#
# + localHost - The local IP address string in textual presentation to which the socket is bound
public client class Client {
 
    private string? localHost = ();
   
    # Initializes the UDP connectionless client based on the provided configurations.
    # ```ballerina
    # udp:Client|udp:Error? socketClient = new(localHost = "localhost");
    # ```
    #
    # + config - Connectionless client related configuration
    public isolated function init(*ClientConfig config) returns Error? {
        return initConnectionlessClient(self, config);
    }

    # Sends the given data to the specified remote host.
    # ```ballerina
    # udp:Error? result = socketClient->send({remoteHost: "localhost", remotePort: 48826, data:"msg".toBytes());
    # ```
    #
    # + datagram - Contains the data to be sent to the remote host socket and the address of the remote host
    # + return - () or else a `udp:Error` if the given data can't be sent
    isolated remote function send(Datagram datagram) returns Error? {
        return externConnectionlessSend(self, datagram);
    }

    # Reads data from the remote host. 
    # ```ballerina
    # udp:Datagram|udp:Error? result = socketClient->receive();
    # ```
    #
    # + return - The Datagram, or else a `udp:Error` if the data can't be read from the remote host
    isolated remote function receive() returns Datagram|Error? {
        return externConnectionlessReceive(self);
    }

    # Free up the occupied socket 
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
# + remoteHost - The hostname of the remote host
# + remotePort - The port number of the remote host
# + data - The content which needs to be transported to the remote host
public type Datagram record {|
   string remoteHost;
   int remotePort;
   readonly byte[] data;
|};

# Configurations for the connectionless udp client
# 
# + localHost - Local binding of the interface
# + timeoutInMillis - The socket reading timeout value to be used in milliseconds. If this is not set,
#                         the default value of 300000 milliseconds (5 minutes) will be used.
public type ClientConfig record {
   int timeoutInMillis = 30000;
   string? localHost = ();
   // can have other socket options
};


isolated function initConnectionlessClient(Client udpClient, ClientConfig config) returns Error? =
@java:Method {
    name: "initEndpoint",
    'class: "org.ballerinalang.stdlib.udp.endpoint.ConnectionlessClientActions"
} external;

isolated function externConectionlessClientClose(Client udpClient) returns Error? =
@java:Method {
    name: "close",
    'class: "org.ballerinalang.stdlib.udp.endpoint.ConnectionlessClientActions"
} external;

isolated function externConnectionlessReceive(Client udpClient) returns Datagram|Error? =
@java:Method {
    name: "receive",
    'class: "org.ballerinalang.stdlib.udp.endpoint.ConnectionlessClientActions"
} external;

isolated function externConnectionlessSend(Client udpClient, Datagram datagram) returns Error? =
@java:Method {
    name: "send",
    'class: "org.ballerinalang.stdlib.udp.endpoint.ConnectionlessClientActions"
} external;

