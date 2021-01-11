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

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.netty.channel.socket.DatagramPacket;

import static org.ballerinalang.stdlib.udp.Constants.ErrorType.GenericError;

/**
 * Represents the util functions of Socket operations.
 *
 * @since 0.985.0
 */
public class Utils {

    /**
     * udp standard library package ID.
     */
    private static Module udpModule = null;

    /**
     * Create Generic udp error with given error message.
     *
     * @param errMsg the error message
     * @return BError instance which contains the error details
     */
    public static BError createSocketError(String errMsg) {
        return ErrorCreator.createDistinctError(GenericError.errorType(), getUdpPackage(),
                StringUtils.fromString(errMsg));
    }

    /**
     * Create udp error with given error type and message.
     *
     * @param type   the error type which cause for this error
     * @param errMsg the error message
     * @return BError instance which contains the error details
     */
    public static BError createSocketError(Constants.ErrorType type, String errMsg) {
        return ErrorCreator.createDistinctError(type.errorType(), getUdpPackage(), StringUtils.fromString(errMsg));
    }

    public static BMap<BString, Object> createDatagram(DatagramPacket datagramPacket) {
        byte[] byteContent = new byte[datagramPacket.content().readableBytes()];
        datagramPacket.content().readBytes(byteContent);

        BMap<BString, Object> datagram = ValueCreator.createRecordValue(getUdpPackage(),
                Constants.DATAGRAM_RECORD);
        datagram.put(StringUtils.fromString(Constants.DATAGRAM_REMOTE_PORT),
                datagramPacket.sender().getPort());
        datagram.put(StringUtils.fromString(Constants.DATAGRAM_REMOTE_HOST),
                StringUtils.fromString(datagramPacket.sender().getHostName()));
        datagram.put(StringUtils.fromString(Constants.DATAGRAM_DATA),
                ValueCreator.createArrayValue(byteContent));
        return datagram;
    }

    /**
     * Gets ballerina udp package.
     *
     * @return udp package.
     */
    public static Module getUdpPackage() {
        return getModule();
    }

    public static void setModule(Environment env) {
        udpModule = env.getCurrentModule();
    }

    public static Module getModule() {
        return udpModule;
    }
}
