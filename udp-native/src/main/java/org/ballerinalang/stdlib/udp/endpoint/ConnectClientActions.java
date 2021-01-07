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
package org.ballerinalang.stdlib.udp.endpoint;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.ballerinalang.stdlib.udp.SocketConstants;
import org.ballerinalang.stdlib.udp.SocketUtils;
import org.ballerinalang.stdlib.udp.UdpClient;
import org.ballerinalang.stdlib.udp.UdpFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Native function implementations of the UDP ConnectionlessClient.
 *
 * @since 1.1.0
 */
public class ConnectClientActions {
    private static final Logger log = LoggerFactory.getLogger(ConnectionlessClientActions.class);

    public static Object initEndpoint(Environment env, BObject client, BString remoteHost, 
                                      int remotePort, BMap<BString, Object> config) {
        final Future balFuture = env.markAsync();

        Object host = config.getNativeData(SocketConstants.CONFIG_LOCALHOST);
        InetSocketAddress localAddress = null;
        if (host == null) {
            // A port number of zero will let the system pick up an ephemeral port in a bind operation.
            localAddress = new InetSocketAddress(0);
        } else {
            String hostname = ((BString) host).getValue();
            localAddress = new InetSocketAddress(hostname, 0);
        }

        long timeout = config.getIntValue(StringUtils.fromString(SocketConstants.CONFIG_READ_TIMEOUT));
        client.addNativeData(SocketConstants.CONFIG_READ_TIMEOUT, timeout);

        InetSocketAddress remoteAddress = new InetSocketAddress(remoteHost.getValue(), remotePort);
        client.addNativeData(SocketConstants.REMOTE_ADDRESS, remoteAddress);

        try {
            UdpClient udpClient = UdpFactory.createUdpClient(localAddress);
            udpClient.connect(remoteAddress, balFuture);
            client.addNativeData(SocketConstants.CONNECT_CLIENT, udpClient);
        } catch (InterruptedException e) {
            balFuture.complete(SocketUtils.createSocketError("Unable to initialize the udp client."));
        }

        balFuture.complete(null);
        return null;
    }

    public static Object read(Environment env, BObject client) {
        final Future balFuture = env.markAsync();

        long readTimeOut = (long) client.getNativeData(SocketConstants.CONFIG_READ_TIMEOUT);
        try {
            UdpClient udpClient = (UdpClient) client.getNativeData(SocketConstants.CONNECT_CLIENT);
            udpClient.receiveData(readTimeOut, balFuture, SocketConstants.CallFrom.CONNECT_CLIENT);
        } catch (InterruptedException e) {
            balFuture.complete(SocketUtils.createSocketError("Error while receiving data"));
        }

        return null;
    }

    public static Object write(Environment env, BObject client, BArray data) {
       final Future balFuture = env.markAsync();

       byte[] byteContent = data.getBytes();
       InetSocketAddress remoteAddress = (InetSocketAddress) client.getNativeData(SocketConstants.REMOTE_ADDRESS);
       DatagramPacket datagramPacket = new DatagramPacket(Unpooled.wrappedBuffer(byteContent), remoteAddress);

        UdpClient udpClient = (UdpClient) client.getNativeData(SocketConstants.CONNECT_CLIENT);
        udpClient.sendData(datagramPacket, balFuture);

        balFuture.complete(null);
        return null;
    }
    
    public static Object close(BObject client) {
        try {
            UdpClient udpClient = (UdpClient) client.getNativeData(SocketConstants.CONNECT_CLIENT);
            udpClient.shutdown();
        } catch (InterruptedException e) {
            log.error("Unable to close the UDP client.", e);
            return SocketUtils.createSocketError("Unable to close the  UDP client. " + e.getMessage());
        }

        return null;
    }
}
