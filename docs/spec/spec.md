# Specification: Ballerina UDP Library

_Owners_: @Maninda @MohamedSabthar @shafreenAnfar  
_Reviewers_: @shafreenAnfar @Maninda  
_Created_: 2020/11/10  
_Updated_: 2021/11/04  
_Issue_: [#2197](https://github.com/ballerina-platform/ballerina-standard-library/issues/2197)

# Introduction
This is the specification for UDP standard library which is used for sending and
receiving data with UDP programmed in the
[Ballerina programming language](https://ballerina.io/), which is an open-source programming language for the cloud
that makes it easier to use, combine, and create network services.

# Contents

1. [Overview](#1-overview)
2. [User Datagram Protocol (UDP)](#2-user-datagram-protocol)
3. [Client](#3-client)
    * 3.1. [Datagram](#31-datagram)
    * 3.2. [Connectionless Client](#32-connectionless-client)
        * 3.2.1. [`init` function](#321-init-function)
        * 3.2.2. [`sendDatagram` function](#322-senddatagram-function)
        * 3.2.3. [`receiveDatagram` function](#323-receivedatagram-function)
        * 3.2.4. [`close` function](#324-close-function)
    * 3.3. [Connection Oriented Client](#33-connection-oriented-client)
        * 3.3.1. [`writeBytes` function](#331-writebytes-function)
        * 3.3.2. [`readBytes` function](#332-readbytes-function)
        * 3.3.3. [`close` function](#333-close-function)
4. [Service](#4-service)
    * 4.1. [Listener](#41-listener)
    * 4.2. [Service](#42-service)
    * 4.3. [Caller](#43-caller)
5. [Samples](#5-samples)
    * 5.1. [Client](#51-client)
        * 5.1.1. [Connectionless Client](#511-connectionless-client)
        * 5.1.2. [Connection Oriented Client](#512-connection-oriented-client)
    * 5.2. [Service](#52-service)

## 1. Overview
This specification elaborates on Basic UDP clients and services/listeners.

## 2. User Datagram Protocol
UDP is the transport layer protocol used for unreliable connectionless data communication across a network.

Ballerina UDP library has the capability of sending and receiving data via UDP protocol using both clients and services.

## 3. Client
Introduce two different clients, one for connectionless scenarios and another for connection oriented scenarios.

### 3.1 Datagram
A self-contained, independent entity of data carrying sufficient information to be routed from the source to the destination nodes without reliance on earlier exchanges between the nodes and the transporting network.

```ballerina
public type Datagram record {|
    string remoteHost;
    int remotePort;
    byte[] data;
|};
```

### 3.2 Connectionless Client
Follows the principles of pure UDP protocol with connectionless data.

```ballerina
public type ClientConfiguration record {
    decimal timeout = 300;
    string localhost?;
}

public isolated client class Client {

    public isolated function init(*ClientConfiguration config) returns udp:Error? {}

    isolated remote function sendDatagram(udp:Datagram datagram) returns udp:Error? {}

    isolated remote function receiveDatagram() returns (readonly & udp:Datagram)|udp:Error {}

    isolated remote function close() returns udp:Error? {}
}
```

#### 3.2.1 `init` function
Binds the client to the host address that is provided in `config`. Otherwise bind the client to localhost with an ephemeral port.

#### 3.2.2 `sendDatagram` function
A blocking method where each execution of this method will result in sending a datagram to the remote host or in error, nothing in between. If the `byte[]` size is too large than what the native networking software can support, the method may or may not return an error. This is entirely dependent on the host machine and the OS.
Following is the list of categorization of Datagram data sizes,
 - IPv4 theoretical size limit 65507 bytes
 - IPv6 theoretical size limit 65536 bytes
 - Practical safe size limit which many protocols use 8192 bytes
 - Max practical safe size limit 512 bytes

#### 3.2.3 `receiveDatagram` function
Listened datagrams are retrieved one-by-one. If an error happens during the receiving, an error is returned.

#### 3.2.4 `close` function
Clears the external-party-related information from the client.

### 3.3 Connection Oriented Client
Is configured so that it only receives datagrams from an external party, and sends datagrams to an external party, using the given remote address. Once connected, datagrams may not be received from or sent to any other address. The client remains connected until it is explicitly disconnected or until it is closed.

```ballerina
public type ConnectClientConfiguration record {
    decimal timeout = 300;
    string localhost?;
}

public isolated client class ConnectClient {

    public isolated function init(string remoteHost, int remotePort, *ConnectClientConfiguration config) returns upd:Error? {}

    isolated remote function writeBytes(byte[] data) returns udp:Error? {}

    isolated remote function readBytes() returns (readonly & byte[])|udp:Error {}

    isolated remote function close() returns udp:Error? {}
}
```

#### 3.3.1 `writeBytes` function
Writes everything in the `data` to the remote server. If the `data`’s `byte[]` is too large it writes multiple `Datagram`s until the size of the `byte[]` is zero.

#### 3.3.2 `readBytes` function
Reads data as `byte[]`s received from the external party. Returns an error if any interruption happens during the receive operation.

#### 3.3.3 `close` function
Clears the external-party-related information from the client.

## 4. Service
A service can listen to a listener to read data from the UDP socket. Following types are defined to implement the UDP listener-based read/write operations.

### 4.1 Listener

```ballerina
public type ListenerConfiguration record {
    string remoteHost?;
    int remotePort?;
    string localHost?;
}

public class Listener {

    public isolated function init(int localPort, *ListenerConfiguration config) returns upd:error? {}

    public isolated function attach(Service s, () name = ()) returns error? {}

    public isolated function detach(Service s) returns error? {}
}
```

### 4.2 Service
```ballerina
public type Service service object {};
```

### 4.3 Caller

```ballerina
public client class Caller {

    public string? remoteHost = ();
    public int? remotePort = ();

    isolated function init() {}

    remote isolated function sendBytes(byte[] data) returns Error? {}

    remote isolated function sendDatagram(udp:Datagram datagram) returns Error? {}
}
```

## 5 Samples

### 5.1 Client

#### 5.1.1 Connectionless Client

```ballerina
public function main() returns error? {
    udp:Client socketClient = check new;
    byte[] data = "Hello from UDP client".toBytes();
    udp:Datagram datagram = {data, remoteHost: "localhost", remortPort: 80};
    check socketClient->sendDatagram(datagram);
    readonly & udp:Datagram result = check socketClient->receiveDatagram();
    check socketClient->close();
}
```

#### 5.1.2 Connection Oriented Client

```ballerina
public function main() returns error? {
    udp:ConnectClient socketClient = check new("remote.host.com", 80);
    byte[] data = "Hello from UDP client".toBytes();
    check socketClient->writeBytes(data);
    readonly & byte[] result = check socketClient->readBytes();
    check socketClient->close();
}
```

### 5.2 Service

```ballerina
service on new udp:Listener(8080) {

    remote function onBytes(readonly & byte[] data) returns byte[]|udp:Error? {
        //echo back
        return data;
    }

    remote function onDatagram(readonly & udp:Datagram datagram, udp:Caller caller) returns udp:Datagram|udp:Error? {
        check caller->sendDatagram(datagram);
        // instead we can directly return datagram too, to echo back
        // return datagram;
    }

    remote function onError(udp:Error err) {
        io:println(err);
    }
}
```
