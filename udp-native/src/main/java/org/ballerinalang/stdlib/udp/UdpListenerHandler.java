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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;


/**
 * {@link UdpListenerHandler} is a ChannelInboundHandler implementation for udp listener.
 */
public class UdpListenerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private UdpService udpService;

    public UdpListenerHandler(UdpService udpService) {
        this.udpService = udpService;
    }


    public void setUdpService(UdpService udpService) {
        this.udpService = udpService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                DatagramPacket datagramPacket) throws Exception {
        MethodDispatcher.invokeRead(udpService, datagramPacket, ctx.channel());
        reRegisterReadTimeoutHandler(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            MethodDispatcher.invokeOnError(udpService, "Read timed out.");
            reRegisterReadTimeoutHandler(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        MethodDispatcher.invokeOnError(udpService, cause.getMessage());
    }

    private void reRegisterReadTimeoutHandler(ChannelHandlerContext ctx) {
        ctx.channel().pipeline().remove(Constants.READ_TIMEOUT_HANDLER);
        ctx.channel().pipeline().addLast(Constants.READ_TIMEOUT_HANDLER, new IdleStateHandler(udpService.getTimeout(),
                0, 0, TimeUnit.MILLISECONDS));
    }
}
