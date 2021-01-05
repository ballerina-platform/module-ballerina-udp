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

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.udp.exceptions.SelectorInitializeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static java.nio.channels.SelectionKey.OP_READ;
import static org.ballerinalang.stdlib.udp.SocketConstants.ErrorType.ReadTimedOutError;
import static org.ballerinalang.stdlib.udp.SocketUtils.getUdpPackage;

/**
 * This will manage the Selector instance and handle the accept, read and write operations.
 *
 * @since 0.985.0
 */
public class SelectorManager {

    private static final Logger log = LoggerFactory.getLogger(SelectorManager.class);

    private Selector selector;
    private ThreadFactory threadFactory = new SocketThreadFactory("udp-selector");
    private ExecutorService executor = null;
    private boolean running = false;
    private boolean executing = true;
    private ConcurrentLinkedQueue<ChannelRegisterCallback> registerPendingSockets = new ConcurrentLinkedQueue<>();
    private final Object startStopLock = new Object();

    private SelectorManager() throws IOException {
        selector = Selector.open();
    }

    /**
     * This will use to hold the SelectorManager singleton object.
     */
    private static class SelectorManagerHolder {
        private static SelectorManager manager;
        static {
            try {
                manager = new SelectorManager();
            } catch (IOException e) {
                throw new SelectorInitializeException("Unable to initialize the selector", e);
            }
        }
    }

    /**
     * This method will return SelectorManager singleton instance.
     *
     * @return {@link SelectorManager} instance
     * @throws SelectorInitializeException when unable to open a selector
     */
    public static SelectorManager getInstance() throws SelectorInitializeException {
        return SelectorManagerHolder.manager;
    }

    /**
     * Add channel to register pending udp queue. Socket registration has to be happen in the same thread
     * that selector loop execute.
     *
     * @param callback A {@link ChannelRegisterCallback} instance which contains the resources,
     *                      packageInfo and A {@link SelectableChannel}.
     */
    public void registerChannel(ChannelRegisterCallback callback) {
        registerPendingSockets.add(callback);
        selector.wakeup();
    }

    /**
     * Unregister the given client channel from the selector instance.
     *
     * @param channel {@link SelectableChannel} that about to unregister.
     */
    public void unRegisterChannel(SelectableChannel channel) {
        final SelectionKey selectionKey = channel.keyFor(selector);
        if (selectionKey != null) {
            selectionKey.cancel();
        }
    }

    /**
     * Start the selector loop.
     */
    public void start() {
        synchronized (startStopLock) {
            if (running) {
                return;
            }
            if (executor == null || executor.isTerminated()) {
                executor = Executors.newSingleThreadExecutor(threadFactory);
            }
            running = true;
            executing = true;
            executor.execute(this::execute);
        }
    }

    private void execute() {
        while (executing) {
            try {
                registerChannels();
                if (selector.select() == 0) {
                    continue;
                }
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    performAction(key);
                }
            } catch (Throwable e) {
                log.error("An error occurred in selector loop: " + e.getMessage(), e);
            }
        }
    }

    /*
    Channel registration has to be done in the same thread that selector loops runs.
     */
    private void registerChannels() {
        ChannelRegisterCallback channelRegisterCallback;
        while ((channelRegisterCallback = registerPendingSockets.poll()) != null) {
            SocketService socketService = channelRegisterCallback.getSocketService();
            try {
                socketService.getSocketChannel()
                        .register(selector, channelRegisterCallback.getInitialInterest(), socketService);
            } catch (ClosedChannelException e) {
                channelRegisterCallback.notifyFailure("udp already closed");
                continue;
            }
            channelRegisterCallback.notifyRegister();
        }
    }


    private void performAction(SelectionKey key) {
        if (!key.isValid()) {
            key.cancel();
        } else if (key.isReadable()) {
            onReadReady(key);
        }
    }

    private void onReadReady(SelectionKey key) {
        SocketService socketService = (SocketService) key.attachment();
        // Remove further interest on future read ready requests until this one is served.
        // This will prevent the busy loop.
        key.interestOps(0);
        // Add to the read ready queue. The content will be read through the caller->read action.
        ReadReadySocketMap.getInstance().add(new SocketReader(socketService, key));
        invokeRead(key.channel().hashCode());
    }

    /**
     * Perform the read operation for the given udp. This will either read data from the udp channel or dispatch
     * to the onReadReady resource if resource's lock available.
     *
     * @param socketHashId udp hash id
     */
    public void invokeRead(int socketHashId) {
        // Check whether there is any caller->read pending action and read ready udp.
        ReadPendingSocketMap readPendingSocketMap = ReadPendingSocketMap.getInstance();
        if (readPendingSocketMap.isPending(socketHashId)) {
            // Lock the ReadPendingCallback instance. This will prevent duplicate invocation that happen from both
            // read action and selector manager sides.
            synchronized (readPendingSocketMap.get(socketHashId)) {
                ReadReadySocketMap readReadySocketMap = ReadReadySocketMap.getInstance();
                if (readReadySocketMap.isReadReady(socketHashId)) {
                    SocketReader socketReader = readReadySocketMap.remove(socketHashId);
                    ReadPendingCallback callback = readPendingSocketMap.remove(socketHashId);
                    readUdpSocket(socketReader, callback);
                }
            }
        }
    }

    private void readUdpSocket(SocketReader socketReader, ReadPendingCallback callback) {
        DatagramChannel channel = (DatagramChannel) socketReader.getSocketService().getSocketChannel();
        try {
            if (callback.getCalledBy() == SocketConstants.CallFrom.CONNECTIONLESS_CLIENT) {
                processConnectionLessClientReceive(socketReader, callback, channel);
            } else if (callback.getCalledBy() == SocketConstants.CallFrom.CONNECT_CLIENT) {
                processConnectClientRead(socketReader, callback, channel);
            }
        } catch (CancelledKeyException | ClosedChannelException e) {
            processError(callback, null, "connection closed");
        } catch (IOException e) {
            log.error("Error while data receive.", e);
            processError(callback, null, e.getMessage());
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            processError(callback, ReadTimedOutError, "error while on receiveFrom operation");
        }
    }

    private void processConnectClientRead(SocketReader socketReader, ReadPendingCallback callback,
                                          DatagramChannel channel) throws IOException {
        ByteBuffer buffer = createBuffer(callback, channel);
        channel.read(buffer);
        callback.resetTimeout();
        final int bufferPosition = buffer.position();
        callback.updateCurrentLength(bufferPosition);
        // Re-register for read ready events.
        socketReader.getSelectionKey().interestOps(OP_READ);
        selector.wakeup();
        byte[] bytes = SocketUtils
                .getByteArrayFromByteBuffer(callback.getBuffer() == null ? buffer : callback.getBuffer());
        callback.getFuture().complete(createUdpSocketReturnValue(callback, bytes));
        callback.cancelTimeout();
    }

    private void processConnectionLessClientReceive(SocketReader socketReader, ReadPendingCallback callback,
                                                    DatagramChannel channel) throws IOException {
        ByteBuffer buffer = createBuffer(callback, channel);
        final InetSocketAddress remoteAddress = (InetSocketAddress) channel.receive(buffer);
        callback.resetTimeout();
        final int bufferPosition = buffer.position();
        callback.updateCurrentLength(bufferPosition);
        // Re-register for read ready events.
        socketReader.getSelectionKey().interestOps(OP_READ);
        selector.wakeup();
        byte[] bytes = SocketUtils
                .getByteArrayFromByteBuffer(callback.getBuffer() == null ? buffer : callback.getBuffer());
        callback.getFuture().complete(createUdpSocketReturnValue(callback, bytes, remoteAddress));
        callback.cancelTimeout();
    }

    private void processError(ReadPendingCallback callback, SocketConstants.ErrorType type, String msg) {
        BError socketError =
                type == null ? SocketUtils.createSocketError(msg) : SocketUtils.createSocketError(type, msg);
        callback.getFuture().complete(socketError);
    }


    private BMap<BString, Object>  createUdpSocketReturnValue(ReadPendingCallback callback, byte[] bytes,
                                                             InetSocketAddress remoteAddress) {
        BMap<BString, Object> datagram = ValueCreator.createRecordValue(getUdpPackage(),
                SocketConstants.DATAGRAM_RECORD);
        datagram.put(StringUtils.fromString(SocketConstants.DATAGRAM_REMOTE_PORT), remoteAddress.getPort());
        datagram.put(StringUtils.fromString(SocketConstants.DATAGRAM_REMOTE_HOST),
                StringUtils.fromString(remoteAddress.getHostName()));
        datagram.put(StringUtils.fromString(SocketConstants.DATAGRAM_DATA), ValueCreator.createArrayValue(bytes));
        return  datagram;
    }

    private BArray createUdpSocketReturnValue(ReadPendingCallback callback, byte[] bytes) {
        return ValueCreator.createArrayValue(bytes);
    }

    private ByteBuffer createBuffer(ReadPendingCallback callback, int osBufferSize) {
        ByteBuffer buffer = ByteBuffer.allocate(osBufferSize);

        return buffer;
    }

    private ByteBuffer createBuffer(ReadPendingCallback callback, DatagramChannel socketChannel)
            throws SocketException {
        return createBuffer(callback, socketChannel.socket().getReceiveBufferSize());
    }


    /**
     * Stop the selector loop.
     *
     * @param graceful whether to shutdown executor gracefully or not
     */
    public void stop(boolean graceful) {
        stop();
        try {
            if (graceful) {
                SocketUtils.shutdownExecutorGracefully(executor);
            } else {
                SocketUtils.shutdownExecutorImmediately(executor);
            }
        } catch (Exception e) {
            log.error("Error occurred while stopping the selector loop: " + e.getMessage(), e);
        }
    }

    private void stop() {
        synchronized (startStopLock) {
            executing = false;
            running = false;
            selector.wakeup();
        }
    }
}
