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

package org.ballerinalang.stdlib.udp;

import io.ballerina.runtime.api.Module;

import static io.ballerina.runtime.api.constants.RuntimeConstants.BALLERINA_BUILTIN_PKG_PREFIX;

/**
 * Constant variable for udp related operations.
 */
public class SocketConstants {
    private SocketConstants() {
    }

    public static final String SOCKET_KEY = "Socket";
    public static final Module SOCKET_PACKAGE_ID = new Module(BALLERINA_BUILTIN_PKG_PREFIX, "udp", "0.8.0");
    public static final String CONFIG_FIELD_HOST = "host";
    public static final String CONFIG_FIELD_PORT = "port";

    public static final String SOCKET_SERVICE = "socketService";
    public static final String IS_CLIENT = "isClient";

    public static final String ADDRESS_RECORD = "Address";
    public static final String DATAGRAM_RECORD = "Datagram";
    public static final String DATAGRAM_REMOTE_ADDRESS = "remoteAddress";
    public static final String DATAGRAM_DATA = "data";

    // If default length pass as the read length then the entire buffer read.
    public static final int DEFAULT_EXPECTED_READ_LENGTH = -100;

    // Default read timeout set as 5 min.
    public static final String READ_TIMEOUT = "readTimeoutInMillis";

    /**
     * Specifies the error type for udp module.
     */
    public enum ErrorType {

        GenericError("GenericError"), ReadTimedOutError("ReadTimedOut");

        private String errorType;

        ErrorType(String errorType) {
            this.errorType = errorType;
        }

        public String errorType() {
            return errorType;
        }
    }

}
