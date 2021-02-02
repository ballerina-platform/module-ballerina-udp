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

import io.ballerina.runtime.api.Future;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 *  {@link UdpClient} creates the udp client and handles all the network operations.
 */
public class UdpClient {

    private Channel channel;
    private final Bootstrap clientBootstrap;

    // create connection oriented client
    public UdpClient(InetSocketAddress localAddress, InetSocketAddress remoteAddress,
                     EventLoopGroup group, Future callback) throws  InterruptedException {
        clientBootstrap = new Bootstrap();
        clientBootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(Constants.CONNECTIONLESS_CLIENT_HANDLER, new UdpClientHandler());
                    }
                });
        if (remoteAddress != null) {
            this.connect(remoteAddress, localAddress, callback);
        }
    }

    // create connection less client
    public UdpClient(InetSocketAddress localAddress, EventLoopGroup group,
                     Future callback) throws  InterruptedException {
        clientBootstrap = new Bootstrap();
        clientBootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(Constants.CONNECTIONLESS_CLIENT_HANDLER, new UdpClientHandler());
                    }
                });
        channel = clientBootstrap.bind(localAddress).sync().channel();
        channel.config().setAutoRead(false);
        callback.complete(null);
    }

    // needed for connection oriented client
    private void connect(SocketAddress remoteAddress, SocketAddress localAddress,
                         Future callback) throws  InterruptedException {
        clientBootstrap.connect(remoteAddress, localAddress)
                .addListener((ChannelFutureListener) future -> {
                    channel = future.channel();
                    channel.pipeline().replace(Constants.CONNECTIONLESS_CLIENT_HANDLER,
                            Constants.CONNECT_CLIENT_HANDLER, new UdpConnectClientHandler());
                    channel.config().setAutoRead(false);
            if (future.isSuccess()) {
                callback.complete(null);
            } else {
                callback.complete(Utils.createSocketError("Can't connect to remote host"));
            }
        });
    }

    public void sendData(DatagramPacket datagram, Future callback) {
        channel.writeAndFlush(datagram).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                callback.complete(null);
            } else {
                callback.complete(Utils.createSocketError("Failed to send data: " + future.cause().getMessage()));
            }
        });
    }

    public void receiveData(long readTimeout, Future callback) {
        channel.pipeline().addFirst(Constants.READ_TIMEOUT_HANDLER, new IdleStateHandler(readTimeout, 0, 0,
                TimeUnit.MILLISECONDS));

        if (channel.pipeline().get(Constants.CONNECTIONLESS_CLIENT_HANDLER) != null) {
            UdpClientHandler handler = (UdpClientHandler) channel.pipeline().
                    get(Constants.CONNECTIONLESS_CLIENT_HANDLER);
            handler.setCallback(callback);
        } else {
            UdpConnectClientHandler handler = (UdpConnectClientHandler) channel.pipeline().
                    get(Constants.CONNECT_CLIENT_HANDLER);
            handler.setCallback(callback);
        }

        channel.read();
    }

    public void close() throws InterruptedException {
        channel.close().sync();
    }
}
