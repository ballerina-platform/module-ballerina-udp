/*
 * Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import io.ballerina.runtime.api.Future;
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

/**
 * Native function implementations of the UDP ConnectionlessClient.
 *
 * @since 1.1.0
 */
public final class Client {

    private Client() {}

    public static Object init(Environment env, BObject client, BMap<BString, Object> config) {
        final Future balFuture = env.markAsync();

        BString host = config.getStringValue(StringUtils.fromString(Constants.CONFIG_LOCALHOST));
        InetSocketAddress localAddress;
        if (host == null) {
            // A port number of zero will let the system pick up an ephemeral port in a bind operation.
            localAddress = new InetSocketAddress(0);
        } else {
            localAddress = new InetSocketAddress(host.getValue(), 0);
        }

        double timeout = ((BDecimal) config.get(StringUtils.fromString(Constants.CONFIG_READ_TIMEOUT))).floatValue();
        client.addNativeData(Constants.CONFIG_READ_TIMEOUT, timeout);

        UdpClient udpClient = UdpFactory.getInstance().createUdpClient(localAddress, balFuture);
        client.addNativeData(Constants.CONNECTIONLESS_CLIENT, udpClient);

        return null;
    }

    public static Object receive(Environment env, BObject client) {
        final Future callback = env.markAsync();

        double readTimeOut = (double) client.getNativeData(Constants.CONFIG_READ_TIMEOUT);
        UdpClient udpClient = (UdpClient) client.getNativeData(Constants.CONNECTIONLESS_CLIENT);
        udpClient.receiveData(readTimeOut, callback);

        return null;
    }

    public static Object send(Environment env, BObject client, BMap<BString, Object> datagram) {
        final Future callback = env.markAsync();

        String host = datagram.getStringValue(StringUtils.fromString(Constants.DATAGRAM_REMOTE_HOST)).getValue();
        int port = datagram.getIntValue(StringUtils.fromString(Constants.DATAGRAM_REMOTE_PORT)).intValue();
        BArray data = datagram.getArrayValue(StringUtils.fromString(Constants.DATAGRAM_DATA));
        byte[] byteContent = data.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(Unpooled.wrappedBuffer(byteContent),
                new InetSocketAddress(host, port));
        UdpClient udpClient = (UdpClient) client.getNativeData(Constants.CONNECTIONLESS_CLIENT);
        udpClient.sendData(datagramPacket, callback);

        return null;
    }

    public static Object close(Environment env, BObject client) {
        final Future callback = env.markAsync();

        UdpClient udpClient = (UdpClient) client.getNativeData(Constants.CONNECTIONLESS_CLIENT);
        udpClient.close(callback);

        return null;
    }
}
