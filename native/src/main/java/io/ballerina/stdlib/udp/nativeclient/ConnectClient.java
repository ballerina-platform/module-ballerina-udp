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

package io.ballerina.stdlib.udp.nativeclient;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.udp.Constants;
import io.ballerina.stdlib.udp.UdpClient;
import io.ballerina.stdlib.udp.UdpFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

import static io.ballerina.stdlib.udp.Utils.getResult;

/**
 * Native function implementations of the UDP ConnectionlessClient.
 *
 * @since 1.1.0
 */
public final class ConnectClient {

    private ConnectClient() {}

    public static Object init(Environment env, BObject client, BString remoteHost,
                              int remotePort, BMap<BString, Object> config) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            BString host = config.getStringValue(StringUtils.fromString(Constants.CONFIG_LOCALHOST));
            InetSocketAddress localAddress;
            if (host == null) {
                // A port number of zero will let the system pick up an ephemeral port in a bind operation.
                localAddress = new InetSocketAddress(0);
            } else {
                localAddress = new InetSocketAddress(host.getValue(), 0);
            }

            double timeout =
                    ((BDecimal) config.get(StringUtils.fromString(Constants.CONFIG_READ_TIMEOUT))).floatValue();
            client.addNativeData(Constants.CONFIG_READ_TIMEOUT, timeout);

            InetSocketAddress remoteAddress = new InetSocketAddress(remoteHost.getValue(), remotePort);
            client.addNativeData(Constants.REMOTE_ADDRESS, remoteAddress);

            UdpClient udpClient = UdpFactory.getInstance().createUdpClient(localAddress, remoteAddress, balFuture);
            client.addNativeData(Constants.CONNECT_CLIENT, udpClient);
            return getResult(balFuture);
        });
    }

    public static Object read(Environment env, BObject client) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            double readTimeOut = (double) client.getNativeData(Constants.CONFIG_READ_TIMEOUT);
            UdpClient udpClient = (UdpClient) client.getNativeData(Constants.CONNECT_CLIENT);
            udpClient.receiveData(readTimeOut, balFuture);
            return getResult(balFuture);
        });
    }

    public static Object write(Environment env, BObject client, BArray data) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            byte[] byteContent = data.getBytes();
            InetSocketAddress remoteAddress = (InetSocketAddress) client.getNativeData(Constants.REMOTE_ADDRESS);
            DatagramPacket datagramPacket = new DatagramPacket(Unpooled.wrappedBuffer(byteContent), remoteAddress);
            UdpClient udpClient = (UdpClient) client.getNativeData(Constants.CONNECT_CLIENT);
            udpClient.sendData(datagramPacket, balFuture);
            return null;
        });
    }

    public static Object close(Environment env, BObject client) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            UdpClient udpClient = (UdpClient) client.getNativeData(Constants.CONNECT_CLIENT);
            udpClient.close(balFuture);
            return getResult(balFuture);
        });
    }
}
