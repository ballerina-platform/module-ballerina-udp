/*
 * Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.udp;

/**
 * Constant variable for udp related operations.
 */
public final class Constants {

    private Constants() {}

    // Constant related to ballerina Datagram record
    public static final String DATAGRAM_RECORD = "Datagram";
    public static final String DATAGRAM_REMOTE_HOST = "remoteHost";
    public static final String DATAGRAM_REMOTE_PORT = "remotePort";
    public static final String DATAGRAM_DATA = "data";

    public static final String CONNECTIONLESS_CLIENT = "client";
    public static final String CONNECT_CLIENT = "connectClient";
    public static final String LISTENER = "listener";

    // Constant related to ballerina ClientConfiguration/ConnectClientConfiguration record
    public static final String LISTENER_CONFIG = "ListenerConfiguration";
    public static final String CONFIG_READ_TIMEOUT = "timeout";
    public static final String CONFIG_LOCALHOST = "localHost";
    public static final String CONFIG_REMOTE_HOST = "remoteHost";
    public static final String CONFIG_REMOTE_PORT = "remotePort";

    // Constant handler names
    public static final String READ_TIMEOUT_HANDLER = "readTimeoutHandler";
    public static final String CONNECTIONLESS_CLIENT_HANDLER = "clientHandler";
    public static final String CONNECT_CLIENT_HANDLER = "connectClientHandler";
    public static final String LISTENER_HANDLER = "udpListenerHandler";

    // Remote method names of ballerina service object
    public static final String ON_BYTES = "onBytes";
    public static final String ON_DATAGRAM = "onDatagram";
    public static final String ON_ERROR = "onError";

    // Constants related to caller
    public static final String CALLER = "Caller";
    public static final String CALLER_REMOTE_HOST = "remoteHost";
    public static final String CALLER_REMOTE_PORT = "remotePort";

    public static final String REMOTE_ADDRESS = "remoteAddress";
    public static final String SERVICE = "service";
    public static final String LOCAL_PORT = "localPort";
    public static final String CHANNEL = "Channel";
    public static final int DATAGRAM_DATA_SIZE = 8192;
    public static final String READ_ONLY_BYTE_ARRAY = "(byte[] & readonly)";
    public static final String READ_ONLY_DATAGRAM = "(udp:Datagram & readonly)";
    public static final String BYTE_ARRAY = "byte[]";
    public static final String ERROR = "Error";
    public static final String UDP = "udp";

    /**
     * Specifies the error type for udp module.
     */
    public enum ErrorType {

        Error("Error");

        private final String errorType;

        ErrorType(String errorType) {
            this.errorType = errorType;
        }

        public String errorType() {
            return errorType;
        }
    }
}
