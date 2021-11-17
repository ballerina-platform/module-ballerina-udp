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

package io.ballerina.stdlib.udp;

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Dispatch async methods.
 */
public class Dispatcher {

    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);

    private static void invokeOnBytes(UdpService udpService, DatagramPacket datagramPacket, Channel channel,
                                      Type[] parameterTypes) {
        try {
            Object[] params = getOnBytesSignature(datagramPacket, channel, parameterTypes);
            StrandMetadata metadata = new StrandMetadata(Utils.getModule().getOrg(), Utils.getModule().getName(),
                    Utils.getModule().getVersion(), Constants.ON_BYTES);
            if (udpService.getService().getType().isIsolated() &&
                        udpService.getService().getType().isIsolated(Constants.ON_BYTES)) {
                udpService.getRuntime().invokeMethodAsyncConcurrently(udpService.getService(), Constants.ON_BYTES,
                        null, metadata, new UdpCallback(udpService, channel, datagramPacket), null, null, params);
            } else {
                udpService.getRuntime().invokeMethodAsyncSequentially(udpService.getService(), Constants.ON_BYTES,
                        null, metadata, new UdpCallback(udpService, channel, datagramPacket), null, null, params);
            }
        } catch (BError e) {
            Dispatcher.invokeOnError(udpService, e.getMessage());
        }
    }

    private static void invokeOnDatagram(UdpService udpService, DatagramPacket datagramPacket, Channel channel,
                                         Type[] parameterTypes) {
        try {
            Object[] params = getOnDatagramSignature(datagramPacket, channel, parameterTypes);
            StrandMetadata metadata = new StrandMetadata(Utils.getModule().getOrg(), Utils.getModule().getName(),
                    Utils.getModule().getVersion(), Constants.ON_DATAGRAM);
            if (udpService.getService().getType().isIsolated() &&
                        udpService.getService().getType().isIsolated(Constants.ON_DATAGRAM)) {
                udpService.getRuntime().invokeMethodAsyncConcurrently(udpService.getService(), Constants.ON_DATAGRAM,
                        null, metadata, new UdpCallback(udpService, channel, datagramPacket), null, null, params);
            } else {
                udpService.getRuntime().invokeMethodAsyncSequentially(udpService.getService(), Constants.ON_DATAGRAM,
                        null, metadata, new UdpCallback(udpService, channel, datagramPacket), null, null, params);
            }
        } catch (BError e) {
            Dispatcher.invokeOnError(udpService, e.getMessage());
        }
    }

    public static void invokeOnError(UdpService udpService, String message) {
        try {
            MethodType methodType = Arrays.stream(udpService.getService().getType().getMethods()).
                    filter(m -> m.getName().equals(Constants.ON_ERROR)).findFirst().orElse(null);
            if (methodType != null) {
                Object params[] = getOnErrorSignature(message);
                StrandMetadata metadata = new StrandMetadata(Utils.getModule().getOrg(), Utils.getModule().getName(),
                        Utils.getModule().getVersion(), Constants.ON_ERROR);
                if (udpService.getService().getType().isIsolated() &&
                            udpService.getService().getType().isIsolated(Constants.ON_ERROR)) {
                    udpService.getRuntime().invokeMethodAsyncConcurrently(udpService.getService(), Constants.ON_ERROR,
                            null, metadata, new UdpCallback(udpService), null, null, params);
                } else {
                    udpService.getRuntime().invokeMethodAsyncSequentially(udpService.getService(), Constants.ON_ERROR,
                            null, metadata, new UdpCallback(udpService), null, null, params);
                }
            }
        } catch (Throwable t) {
            log.error("Error while executing onError function", t);
        }
    }

    private static Object[] getOnBytesSignature(DatagramPacket datagramPacket, Channel channel, Type[] parameterTypes) {
        byte[] byteContent = new byte[datagramPacket.content().readableBytes()];
        datagramPacket.content().readBytes(byteContent);

        Object[] bValues = new Object[parameterTypes.length * 2];
        int index = 0;
        for (Type param : parameterTypes) {
            int paramTag = param.getTag();
            switch (paramTag) {
                case TypeTags.INTERSECTION_TAG:
                    bValues[index++] = ValueCreator.createArrayValue(byteContent);
                    bValues[index++] = true;
                    break;
                case TypeTags.OBJECT_TYPE_TAG:
                    bValues[index++] = createClient(datagramPacket, channel);
                    bValues[index++] = true;
                    break;
                default:
                    break;
            }
        }
        return bValues;
    }

    private static Object[] getOnDatagramSignature(DatagramPacket datagramPacket, Channel channel,
                                                   Type[] parameterTypes) {
        Object[] bValues = new Object[parameterTypes.length * 2];
        int index = 0;
        for (Type param : parameterTypes) {
            int paramTag = param.getTag();
            switch (paramTag) {
                case TypeTags.INTERSECTION_TAG:
                    bValues[index++] = Utils.createReadOnlyDatagramWithSenderAddress(datagramPacket);
                    bValues[index++] = true;
                    break;
                case TypeTags.OBJECT_TYPE_TAG:
                    bValues[index++] = createClient(datagramPacket, channel);
                    bValues[index++] = true;
                    break;
                default:
                    break;
            }
        }
        return bValues;
    }

    private static Object[] getOnErrorSignature(String message) {
        return new Object[]{Utils.createUdpError(message), true};
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
                    Dispatcher.invokeOnBytes(udpService, datagramPacket, channel,
                            method.getType().getParameterTypes());
                    break;
                case Constants.ON_DATAGRAM:
                    Dispatcher.invokeOnDatagram(udpService, datagramPacket, channel,
                            method.getType().getParameterTypes());
                    break;
                default:
                    break;
            }
        }
    }
}
