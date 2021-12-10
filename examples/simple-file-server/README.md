# Simple File Server

## Overview

This guide explains how to put and get a file to/from an UDP server using Ballerina.
The below are the detailed explanations of each of the steps of this example.

### Step 1 - Start the UDP Server

Execute the run command of the Ballerina Server project of the sample UDP server. This would start the UDP server.

### Step 2 - Initialize the UDP Client with Credentials

In order to interact with a UDP server, a UDP Client has to be initialized with the server related connection details and user's credentials. The Ballerina UDP client is configured to run on port the `20211` of `localhost`.

### Step 3 - Send the File

A blocked stream of bytes of the local file is created and passed to the UDP client along with the destination path name.
Then the file is stored in the UDP file server.

### Step 4 - Close the File Stream

When all the file related operations are finished, the byte stream corresponding to the received file is closed.


## Testing

You can run the above code in your local environment. Navigate to the directory
[`examples/simple-file-server/server`](./server) and execute the command below.
```shell
$ bal run
```

The successful execution of the server should show the output below.
```shell
Compiling source
	udp/udp_file_server:1.0.0

Running executable
```

Then navigate to the [`examples/simple-file-server/client`](./client) directory, and execute the command below.
```shell
$ bal run
```

The successful execution of the client should show the output below.
```shell
Compiling source
	udp/udp_file_client:1.0.0

Running executable
```

Now, check the current directory for the received file. The file, `dest.txt` should be available.
