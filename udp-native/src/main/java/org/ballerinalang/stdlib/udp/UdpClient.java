package org.ballerinalang.stdlib.udp;

import io.ballerina.runtime.api.Future;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 *  {@link UdpClient} creates the udp client and handles all the network operations.
 *
 */
public class UdpClient {

    private final EventLoopGroup group;
    private final Channel channel;

    public UdpClient(InetSocketAddress localAddress) throws  InterruptedException {
        group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception { }
                });
        channel = b.bind(localAddress).sync().channel();
        channel.config().setAutoRead(false);
    }

    // needed for connection oriented client
    public void connect(SocketAddress remoteAddress, Future callback) throws  InterruptedException {
        channel.connect(remoteAddress).sync().addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                callback.complete(SocketUtils.createSocketError("Can't connect to remote host"));
            }
        });
    }

    public void sendData(DatagramPacket datagram, Future callback) {
        channel.writeAndFlush(datagram).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                callback.complete(SocketUtils.createSocketError("Failed to send data"));
            }
        });
    }

    public void receiveData(long readTimeout, Future callback, SocketConstants.CallFrom callFrom)
            throws InterruptedException {
        channel.deregister().sync();
        channel.pipeline().addLast(new IdleStateHandler(0, 0, readTimeout,
                TimeUnit.MILLISECONDS));
        channel.pipeline().addLast(new UdpClientHandler(callback, callFrom));
        channel.eventLoop().register(channel);
        channel.read();
    }

    public void shutdown() throws InterruptedException {
        group.shutdownGracefully();
    }
}
