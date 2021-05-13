## Overview

This module provides an implementation for sending/receiving datagrams to/from another application process (local or remote) using UDP.

When the local host address or the IP address is not given to the optional `localHost` field, `localhost` address is bound by default.
Port number of the local port which is used to connect to the remote server is determined randomly. (ephemeral port)

#### Client
The `udp:Client` is used to interact with the remote UDP host and it can be defined as follows:

```ballerina
udp:Client socketClient = check new;

udp:Datagram datagram = {
    remoteHost: "localhost",
    remotePort : 48829,
    data : "Hello Ballerina".toBytes()
};

check socketClient->sendDatagram(datagram);

readonly & udp:Datagram result = check socketClient->receiveDatagram();

check socketClient->close();
```

#### ConnectClient
The `udp:ConnectClient` is configured by providing `remoteHost` and `remotePort`; so that it only receives data from, and sends data to, the configured remote host. Once connected, data may not be received from or sent to any other hosts. The client remains connected until it is explicitly it is closed.
If the number of bytes given to the `writeBytes` method is greater than the data size allowed by a datagram, it will iteratively send all the bytes with several datagrams.

```ballerina
udp:ConnectClient socketClient = check new("localhost", 48829);

string msg = "Hello Ballerina";
check socketClient->writeBytes(msg.toBytes());

readonly & byte[] result = check socketClient->readBytes();

check socketClient->close();
```

#### Listener
The `udp:Listener` is used to listen to the incoming socket request.<br/>

The `udp:Listener` can have following methods
- `onBytes(readonly & byte[] data, udp:Caller caller)` or `onDatagram(readonly & udp:Datagram, udp:Caller)` - These remote method gets invoked once the content is received from the client. The client is represented using the `udp:Caller`.
- `onError(readonly & udp:Error err)` - This remote method is invoked in an error situation.

A `udp:Listener`can be defined as follows:

```ballerina
service on new udp:Listener(48829) {
    remote function onDatagram(readonly & udp:Datagram datagram)
            returns udp:Datagram|udp:Error? {
        // Handle the content received from the client
    }

    remote function onError(udp:Error err) {
        // Handle the error situation
    }
}
```
