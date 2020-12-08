## Module Overview

This module provides an implementation for sending/receiving messages to/from another application process (local or remote) for connectionless protocols.

#### UDP Client
The `socket:UdpClient` is used to interact with the remote UDP host and it can be defined as follows:
```ballerina
socket:UdpClient socketClient = new;
string msg = "Hello from UDP client";
byte[] message = msg.toBytes();
int|socket:Error sendResult = socketClient->sendTo(message, { host: "localhost", port: 48826 });
```

For information on the operations, which you can perform with this module, see the below **Functions**. For examples on the usage of the operations, see the following.
 * [Basic UDP Client Socket Example](https://ballerina.io/learn/by-example/udp-socket-client.html)
