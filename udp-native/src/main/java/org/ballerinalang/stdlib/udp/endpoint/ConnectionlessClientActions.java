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
package org.ballerinalang.stdlib.udp.endpoint;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.udp.ChannelRegisterCallback;
import org.ballerinalang.stdlib.udp.ReadPendingCallback;
import org.ballerinalang.stdlib.udp.ReadPendingSocketMap;
import org.ballerinalang.stdlib.udp.SelectorManager;
import org.ballerinalang.stdlib.udp.SocketConstants;
import org.ballerinalang.stdlib.udp.SocketService;
import org.ballerinalang.stdlib.udp.SocketUtils;
import org.ballerinalang.stdlib.udp.exceptions.SelectorInitializeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;

import static java.nio.channels.SelectionKey.OP_READ;
import static org.ballerinalang.stdlib.udp.SocketConstants.IS_CLIENT;
import static org.ballerinalang.stdlib.udp.SocketConstants.SOCKET_KEY;
import static org.ballerinalang.stdlib.udp.SocketConstants.SOCKET_SERVICE;

/**
 * Native function implementations of the UDP ConnectionlessClient.
 *
 * @since 1.1.0
 */
public class ConnectionlessClientActions {
    private static final Logger log = LoggerFactory.getLogger(ConnectionlessClientActions.class);

    public static Object initEndpoint(Environment env, BObject client, BMap<BString, Object> config) {
        final Future balFuture = env.markAsync();
        SelectorManager selectorManager;
        SocketService socketService;
        try {
            DatagramChannel socketChannel = DatagramChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.socket().setReuseAddress(true);
            client.addNativeData(SOCKET_KEY, socketChannel);
            client.addNativeData(IS_CLIENT, true);
            //  A port number of zero will let the system pick up an ephemeral port in a bind operation.
           Object host = config.getNativeData(SocketConstants.LOCALHOST);
            if (host == null) {
                socketChannel.bind(new InetSocketAddress(0));
            } else {
                String hostname = ((BString) host).getValue();
                socketChannel.bind(new InetSocketAddress(hostname, 0));
            }
            long timeout = config.getIntValue(StringUtils.fromString(SocketConstants.READ_TIMEOUT));
            socketService = new SocketService(socketChannel, env.getRuntime(), timeout);
            client.addNativeData(SOCKET_SERVICE, socketService);
            selectorManager = SelectorManager.getInstance();
            selectorManager.start();
        } catch (SelectorInitializeException e) {
            log.error(e.getMessage(), e);
            balFuture.complete(SocketUtils.createSocketError("Unable to initialize the selector."));
            return null;
        } catch (SocketException e) {
            balFuture.complete(SocketUtils.createSocketError("Unable to bind to a local port."));
            return null;
        } catch (IOException e) {
            log.error("Unable to initiate the client udp", e);
            balFuture.complete(SocketUtils.createSocketError("Unable to initiate the udp client: " + e.getMessage()));
            return null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            balFuture.complete(SocketUtils.createSocketError("Unable to start the udp client."));
            return null;
        }
        selectorManager.registerChannel(new ChannelRegisterCallback(socketService, balFuture, OP_READ));
        return null;
    }

    public static Object receive(Environment env, BObject client) {
        final Future balFuture = env.markAsync();
        DatagramChannel socket = (DatagramChannel) client.getNativeData(SocketConstants.SOCKET_KEY);
        int socketHash = socket.hashCode();
        SocketService socketService = (SocketService) client.getNativeData(SocketConstants.SOCKET_SERVICE);
        ReadPendingCallback readPendingCallback = new ReadPendingCallback(balFuture, socketHash,
                socketService.getReadTimeout());
        ReadPendingSocketMap.getInstance().add(socket.hashCode(), readPendingCallback);
        log.debug("Notify to invokeRead");
        SelectorManager.getInstance().invokeRead(socketHash);
        return null;
    }

    public static Object send(BObject client, BMap<BString, Object> datagram) {
        DatagramChannel socket = (DatagramChannel) client.getNativeData(SocketConstants.SOCKET_KEY);
        BArray data = datagram.getArrayValue(StringUtils.fromString(SocketConstants.DATAGRAM_DATA));
        String host = datagram.getStringValue(
                StringUtils.fromString(SocketConstants.DATAGRAM_REMOTE_HOST)).getValue();
        int port = datagram.getIntValue(StringUtils.fromString(SocketConstants.DATAGRAM_REMOTE_PORT)).intValue();
        byte[] byteContent = data.getBytes();
        if (log.isDebugEnabled()) {
            log.debug(String.format("No of byte going to write[%d]: %d", socket.hashCode(), byteContent.length));
        }
        try {
            final InetSocketAddress remote = new InetSocketAddress(host, port);
            int write = socket.send(ByteBuffer.wrap(byteContent), remote);
            if (log.isDebugEnabled()) {
                log.debug(String.format("No of byte written for the client[%d]: %d", socket.hashCode(), write));
            }
            return null;
        } catch (ClosedChannelException e) {
            return SocketUtils.createSocketError("UDP client is already closed .");
        } catch (IOException e) {
            log.error("Unable to perform write[" + socket.hashCode() + "]", e);
            return SocketUtils.createSocketError("write failed. " + e.getMessage());
        }
    }
    
    public static Object close(BObject client) {
        final DatagramChannel socketChannel = (DatagramChannel) client.getNativeData(SOCKET_KEY);
        try {
            // SocketChannel can be null if something happen during the onConnect. Hence the null check.
            if (socketChannel != null) {
                socketChannel.close();
                SelectorManager.getInstance().unRegisterChannel(socketChannel);
            }
            // This need to handle to support multiple client close.
            if (Boolean.parseBoolean(client.getNativeData(IS_CLIENT).toString())) {
                SelectorManager.getInstance().stop(true);
            }
        } catch (IOException e) {
            log.error("Unable to close the UDP client.", e);
            return SocketUtils.createSocketError("Unable to close the  UDP client. " + e.getMessage());
        }
        return null;
    }
}
