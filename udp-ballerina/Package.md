## Package Overview

This package provides an implementation for sending/receiving messages to/from another application process (local or remote) for connectionless protocols.

#### Client
The `udp:Client` is used to interact with the remote UDP host and it can be defined as follows:

```ballerina
import ballerina/udp;

public function main() returns error? {
    udp:Client socketClient = check new;

    udp:Datagram datagram = {
        remoteHost: "localhost",
        remotePort : 48829,
        data : "Hello Ballerina".toBytes()
    };

    check socketClient->sendDatagram(datagram);

    readonly & udp:Datagram result = check socketClient->receiveDatagram();

    check socketClient->close();
}
```

#### ConnectClient
The `udp:ConnectClient` is configured by providing `remoteHost` and `remotePort`; so that it only receives data from, and sends data to, the configured remote host. Once connected, data may not be received from or sent to any other hosts. The client remains connected until it is explicitly it is closed.

```ballerina
import ballerina/udp;

public function main() returns error? {
    udp:ConnectClient socketClient = check new("localhost", 48829);

    string msg = "Hello Ballerina";
    check socketClient->writeBytes(msg.toBytes());

    readonly & byte[] result = check socketClient->readBytes();

    check socketClient->close();
}
```

#### Listener
The `udp:Listener` is used to listen to the incoming socket request.<br/>

The `udp:Listener` can have following methods<br/>
`onBytes(readonly & byte[] data, udp:Caller caller)` or `onDatagram(readonly & udp:Datagram, udp:Caller)` - These remote method gets invoked once the content is received from the client. The client is represented using the `udp:Caller`.<br/>
`onError(readonly & udp:Error err)` - This remote method is invoked in an error situation.

A `udp:Listener`can be defined as follows:

```ballerina
import ballerina/udp;

service on new udp:Listener(48829) {
    remote function onBytes(readonly & byte[] data, udp:Caller caller) 
            returns (readonly & byte[])|udp:Error? {
        // echo back the data to the same caller
        return data;
    }

    remote function onError(readonly & udp:Error err) {
        log:printError("An error occured", 'error = err);
    }
}
```

For information on the operations, which you can perform with this package, see the below **Functions**. For examples on the usage of the operations, see the following.
 * [Basic UDP Client Example](https://ballerina.io/learn/by-example/udp-client.html)
 * [Basic UDP ConnectClient Example](https://ballerina.io/learn/by-example/udp-connect-client.html)
 * [Basic UDP Listener Example](https://ballerina.io/learn/by-example/udp-listener.html)
