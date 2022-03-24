Ballerina UDP Library
===================

  [![Build](https://github.com/ballerina-platform/module-ballerina-udp/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-udp/actions/workflows/build-timestamped-master.yml)
  [![Trivy](https://github.com/ballerina-platform/module-ballerina-udp/actions/workflows/trivy-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-udp/actions/workflows/trivy-scan.yml)  
  [![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-udp.svg)](https://github.com/ballerina-platform/module-ballerina-udp/commits/master)
  [![Github issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-standard-library/module/udp.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-standard-library/labels/module%2Fudp)
  [![codecov](https://codecov.io/gh/ballerina-platform/module-ballerina-udp/branch/main/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerina-udp)

This library provides APIs for sending/receiving messages to/from another application process (local or remote) for connectionless protocols.

When the local host address or the IP address is not given to the optional `localHost` field, the `localhost` address is bound by default.
The port number of the local port, which is used to connect to the remote server is determined randomly (ephemeral port).

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
The `udp:ConnectClient` is configured by providing the `remoteHost` and `remotePort` so that it only receives data from and sends data to the configured remote host. Once connected, data may not be received from or sent to any other hosts. The client remains connected until it is explicitly closed.
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

The `udp:Listener` can have the following methods.

**`onBytes(readonly & byte[] data, udp:Caller caller)` or `onDatagram(readonly & udp:Datagram, udp:Caller)`**: These remote methods get invoked once the content is received from the client. The client is represented using the `udp:Caller`.

**`onError(readonly & udp:Error err)`**: This remote method is invoked in an error situation.

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

## Issues and projects 

Issues and Projects tabs are disabled for this repository as this is part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc. please visit Ballerina Standard Library [parent repository](https://github.com/ballerina-platform/ballerina-standard-library). 

This repository only contains the source code for the package.

## Building from the source

### Setting up the prerequisites

1. Download and install Java SE Development Kit (JDK) version 11 (from one of the following locations).

   * [Oracle](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
   
   * [OpenJDK](https://adoptopenjdk.net/)
   
        > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.
     

### Building the source

Execute the commands below to build from the source.

1. To build the package:
   ```    
   ./gradlew clean build
   ```

2. To run the tests:
   ```
   ./gradlew clean test
   ```

3. To run a group of tests
   ```
   ./gradlew clean test -Pgroups=<test_group_names>
   ```

4. To build the without the tests:
   ```
   ./gradlew clean build -x test
   ```

5. To debug package implementation:
   ```
   ./gradlew clean build -Pdebug=<port>
   ```

6. To debug with Ballerina language:
   ```
   ./gradlew clean build -PbalJavaDebug=<port>
   ```

7. Publish the generated artifacts to the local Ballerina central repository:
    ```
    ./gradlew clean build -PpublishToLocalCentral=true
    ```

8. Publish the generated artifacts to the Ballerina central repository:
   ```
   ./gradlew clean build -PpublishToCentral=true
   ```
      
## Contributing to Ballerina

As an open source project, Ballerina welcomes contributions from the community. 

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For more information go to the [`udp` library](https://lib.ballerina.io/ballerina/udp/latest).
* For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
* Chat live with us via our [Slack channel](https://ballerina.io/community/slack/).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
