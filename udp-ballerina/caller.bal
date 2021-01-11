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
// under the License.

import ballerina/java;

# Represents caller object in udp service remote methods
# 
# + remoteHost - The hostname of the remote host
# + remotePort - The port number of the remote host
public client class Caller {

  public string? remoteHost = ();
  public int? remotePort = ();

  isolated function init(){
    // package level private init() to prevent object creation
  }
  
  # Sends the response as byte[] to the same remote host.
  # 
  # + data - The data need to be sent to the remote host
  # + return - () or else a `udp:Error` if the given data can't be sent
  remote isolated function sendBytes(byte[] data) returns Error? {
    return externSendBytes(self, data);
  }

  # Sends the response as datagram to a remote destination as
  # specified in datagram.
  # 
  # + datagram - Contains the data to be sent to the remote host
  #              and the address of the remote host
  # + return - () or else a `udp:Error` if the given data can't be sent
  remote isolated function sendDatagram(Datagram datagram) returns Error? {
    return externSendDatagram(self, datagram);
  }
}

isolated function externSendBytes(Caller caller, byte[] data) returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.udp.nativelistener.Caller",
    name: "sendBytes"
} external;

isolated function externSendDatagram(Caller caller, Datagram datagram) returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.udp.nativelistener.Caller",
    name: "sendDatagram"
} external;
