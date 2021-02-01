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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * {@link UdpListener} creates the udp client and handles all the network operations.
 */
public class UdpListener {

    private Channel channel;
    private final Bootstrap listenerBootstrap;

    public UdpListener(InetSocketAddress localAddress, InetSocketAddress remoteAddress,
                       EventLoopGroup group, Future callback, UdpService udpService) throws InterruptedException {
        listenerBootstrap = new Bootstrap();
        listenerBootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(Constants.LISTENER_HANDLER, new UdpListenerHandler(udpService));
                    }
                });
        if (remoteAddress != null) {
            connect(remoteAddress, localAddress, callback);
        } else {
            channel = listenerBootstrap.bind(localAddress).sync().channel();
            callback.complete(null);
        }
    }

    // invoke when caller call writeBytes() or sendDatagram()
    public static void send(DatagramPacket datagram, Channel channel, Future callback) {
        channel.writeAndFlush(datagram).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                callback.complete(null);
            } else {
                callback.complete(Utils.createSocketError("Failed to send data."));
            }
        });
    }

    // invoke when service return byte[] or Datagram
    public static void send(UdpService udpService, DatagramPacket datagram, Channel channel) {
        channel.writeAndFlush(datagram).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                Dispatcher.invokeOnError(udpService, "Failed to send data.");
            }
        });
    }

    // only invoke if the listener is a connected listener
    private void connect(SocketAddress remoteAddress, SocketAddress localAddress, Future callback)
            throws InterruptedException {
        ChannelFuture channelFuture = listenerBootstrap.connect(remoteAddress).sync();
        channel = channelFuture.channel();
        channel = listenerBootstrap.bind(localAddress).sync().channel();
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                callback.complete(null);
            } else {
                callback.complete(Utils.createSocketError("Can't connect to remote host."));
            }
        });
    }

    public void close(Future callback) throws InterruptedException {
        channel.close().sync().addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                callback.complete(null);
            } else {
                callback.complete(Utils.createSocketError("Failed to gracefully shutdown the Listener."));
            }
        });
    }
}
