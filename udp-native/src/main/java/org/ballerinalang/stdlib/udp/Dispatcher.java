/*
 * Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatch async methods.
 */
public class Dispatcher {

    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);

    private static void invokeOnBytes(UdpService udpService, DatagramPacket datagramPacket, Channel channel) {
        try {
            Object[] params = getOnBytesSignature(datagramPacket, channel);

            udpService.getRuntime().invokeMethodAsync(udpService.getService(), Constants.ON_BYTES, null, null,
                    new UdpCallback(), params);
        } catch (BError e) {
            Dispatcher.invokeOnError(udpService, e.getMessage());
        }
    }

    private static void invokeOnDatagram(UdpService udpService, DatagramPacket datagramPacket, Channel channel) {
        try {
            Object[] params = getOnDatagramSignature(datagramPacket, channel);

            udpService.getRuntime().invokeMethodAsync(udpService.getService(), Constants.ON_DATAGRAM, null, null,
                    new UdpCallback(), params);
        } catch (BError e) {
            Dispatcher.invokeOnError(udpService, e.getMessage());
        }
    }


    public static void invokeOnError(UdpService udpService, String message) {
        try {
            Object params[] = getOnErrorSignature(message);

            udpService.getRuntime().invokeMethodAsync(udpService.getService(), Constants.ON_ERROR, null, null,
                    new UdpCallback(udpService), params);
        } catch (Throwable t) {
            log.error("Error while executing onError function", t);
        }
    }

    private static Object[] getOnBytesSignature(DatagramPacket datagramPacket, Channel channel) {
        BObject caller = createClient(datagramPacket, channel);
        byte[] byteContent = new byte[datagramPacket.content().readableBytes()];
        datagramPacket.content().readBytes(byteContent);
        BArray bytes = ValueCreator.createArrayValue(byteContent);

        return new Object[]{bytes, true, caller, true};
    }

    private static Object[] getOnDatagramSignature(DatagramPacket datagramPacket, Channel channel) {
        BObject caller = createClient(datagramPacket, channel);
        return new Object[]{Utils.createDatagram(datagramPacket), true, caller, true};
    }

    private static Object[] getOnErrorSignature(String message) {
        return new Object[]{Utils.createSocketError(message), true};
    }

    private static BObject createClient(DatagramPacket datagramPacket, Channel channel) {
        final BObject caller = ValueCreator.createObjectValue(Utils.getUdpPackage(), Constants.CALLER);
        caller.set(StringUtils.fromString(Constants.CALLER_REMOTE_PORT), datagramPacket.sender().getPort());
        caller.set(StringUtils.fromString(Constants.CALLER_REMOTE_HOST),
                StringUtils.fromString(datagramPacket.sender().getHostName()));
        caller.addNativeData(Constants.CHANNEL, channel);
        return caller;
    }

    public static void invokeRead(UdpService udpService, DatagramPacket datagramPacket, Channel channel) {
        for (MethodType method : udpService.getService().getType().getMethods()) {
            switch (method.getName()) {
                case Constants.ON_BYTES:
                    Dispatcher.invokeOnBytes(udpService, datagramPacket, channel);
                    break;
                case Constants.ON_DATAGRAM:
                    Dispatcher.invokeOnDatagram(udpService, datagramPacket,
                            channel);
                    break;
            }
        }
    }
}
