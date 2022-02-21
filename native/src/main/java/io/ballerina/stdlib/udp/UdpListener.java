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

import io.ballerina.runtime.api.Future;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.PromiseCombiner;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedList;

/**
 * {@link UdpListener} creates the udp client and handles all the network operations.
 */
public class UdpListener {

    private Channel channel;
    private final Bootstrap listenerBootstrap;

    public UdpListener(InetSocketAddress localAddress, InetSocketAddress remoteAddress,
                       EventLoopGroup group, Future callback, UdpService udpService) {
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
            listenerBootstrap.bind(localAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    channel = future.channel();
                    callback.complete(null);
                } else {
                    callback.complete(Utils.createUdpError("Unable to initialize UDP Listener: " +
                            future.cause().getMessage()));
                }
            });
        }
    }

    // invoke when caller call writeBytes() or sendDatagram()
    public static void send(DatagramPacket datagram, Channel channel, Future callback) {
        LinkedList<DatagramPacket> fragments = Utils.fragmentDatagram(datagram);
        PromiseCombiner promiseCombiner = getPromiseCombiner(fragments, channel);

        promiseCombiner.finish(channel.newPromise().addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                callback.complete(null);
            } else {
                callback.complete(Utils
                        .createUdpError("Failed to send data: " + future.cause().getMessage()));
            }
        }));
    }

    // invoke when service return byte[] or Datagram
    public static void send(UdpService udpService, DatagramPacket datagram, Channel channel) {
        LinkedList<DatagramPacket> fragments = Utils.fragmentDatagram(datagram);
        PromiseCombiner promiseCombiner = getPromiseCombiner(fragments, channel);

        promiseCombiner.finish(channel.newPromise().addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                Dispatcher.invokeOnError(udpService, "Failed to send data.");
            }
        }));
    }

    private static PromiseCombiner getPromiseCombiner(LinkedList<DatagramPacket> fragments, Channel channel) {
        PromiseCombiner promiseCombiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        while (fragments.size() > 0) {
            if (channel.isWritable()) {
                promiseCombiner.add(channel.writeAndFlush(fragments.poll()));
            }
        }
        return promiseCombiner;
    }

    // only invoke if the listener is a connected listener
    private void connect(SocketAddress remoteAddress, SocketAddress localAddress, Future callback) {
        listenerBootstrap.connect(remoteAddress, localAddress).addListener((ChannelFutureListener) future -> {
            channel = future.channel();
            if (future.isSuccess()) {
                callback.complete(null);
            } else {
                callback.complete(Utils.createUdpError("Can't connect to remote host."));
            }
        });
    }

    public void close(Future callback) throws InterruptedException {
        if (channel != null) {
            channel.close().sync().addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    callback.complete(null);
                } else {
                    callback.complete(Utils.createUdpError("Failed to gracefully shutdown the Listener."));
                }
            });
        }
    }
}
