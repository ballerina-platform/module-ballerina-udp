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

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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

    public static BError createUdpError(String errMsg) {
        return ErrorCreator.createError(getUdpPackage(), Constants.ErrorType.Error.errorType(),
                StringUtils.fromString(errMsg), null, null);
    }

    public static BMap<BString, Object> createReadOnlyDatagramWithSenderAddress(DatagramPacket datagramPacket) {
        byte[] byteContent = new byte[datagramPacket.content().readableBytes()];
        datagramPacket.content().readBytes(byteContent);
        Map<String, Object> datagramContent = new HashMap<>();
        datagramContent.put(Constants.DATAGRAM_REMOTE_PORT, datagramPacket.sender().getPort());
        datagramContent.put(Constants.DATAGRAM_REMOTE_HOST, StringUtils
                .fromString(datagramPacket.sender().getHostName()));
        datagramContent.put(Constants.DATAGRAM_DATA, ValueCreator.createArrayValue(byteContent));
        BMap<BString, Object> datagram = ValueCreator.createReadonlyRecordValue(getUdpPackage(),
                Constants.DATAGRAM_RECORD, datagramContent);
        return datagram;
    }

    static BMap<BString, Object> createReadonlyDatagramWithRecipientAddress(DatagramPacket datagramPacket) {
        byte[] byteContent = new byte[datagramPacket.content().readableBytes()];
        datagramPacket.content().readBytes(byteContent);
        Map<String, Object> datagramContent = new HashMap<>();
        datagramContent.put(Constants.DATAGRAM_REMOTE_PORT, datagramPacket.recipient().getPort());
        datagramContent.put(Constants.DATAGRAM_REMOTE_HOST, StringUtils
                .fromString(datagramPacket.recipient().getHostName()));
        datagramContent.put(Constants.DATAGRAM_DATA, ValueCreator.createArrayValue(byteContent));
        BMap<BString, Object> datagram = ValueCreator.createReadonlyRecordValue(getUdpPackage(),
                Constants.DATAGRAM_RECORD, datagramContent);
        return datagram;
    }

    static BArray getReadonlyBytesFromDatagram(DatagramPacket datagramPacket) {
        byte[] byteContent = new byte[datagramPacket.content().readableBytes()];
        datagramPacket.content().readBytes(byteContent);
        return ValueCreator.createReadonlyArrayValue(byteContent);
    }

    static LinkedList<DatagramPacket> fragmentDatagram(DatagramPacket datagram) {
        ByteBuf content = datagram.content();
        int contentSize = content.readableBytes();
        LinkedList<DatagramPacket> fragments = new LinkedList<>();

        while (contentSize > 0) {
            if (contentSize > Constants.DATAGRAM_DATA_SIZE) {
                fragments.add(datagram.replace(content.readBytes(Constants.DATAGRAM_DATA_SIZE)));
                contentSize -= Constants.DATAGRAM_DATA_SIZE;
            } else {
                fragments.add(datagram.replace(datagram.content().readBytes(contentSize)));
                contentSize = 0;
            }
        }
        return fragments;
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
