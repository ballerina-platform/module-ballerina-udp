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
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.timeout.IdleStateEvent;

import static org.ballerinalang.stdlib.udp.SocketUtils.getUdpPackage;

/**
 *  {@link UdpClientHandler} ia a ChannelInboundHandler implementation for udp client.
 */
public class UdpClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private Future callback;

    public UdpClientHandler(){}

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                DatagramPacket datagramPacket) throws Exception {
        callback.complete(returnDatagram(datagramPacket));
        ctx.channel().pipeline().remove(SocketConstants.READ_TIMEOUT_HANDLER);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            // return timeout error
            callback.complete(SocketUtils.createSocketError(SocketConstants.ErrorType.ReadTimedOutError,
                    "Read timed out"));
            ctx.channel().pipeline().remove(SocketConstants.READ_TIMEOUT_HANDLER);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        callback.complete(SocketUtils.createSocketError(cause.getMessage()));
        ctx.channel().pipeline().remove(SocketConstants.READ_TIMEOUT_HANDLER);
    }

    public void setCallback(Future callback) {
        this.callback = callback;
    }

    private BMap<BString, Object> returnDatagram(DatagramPacket datagramPacket) {
        byte[] byteContent = new byte[datagramPacket.content().readableBytes()];
        datagramPacket.content().readBytes(byteContent);

        BMap<BString, Object> datagram = ValueCreator.createRecordValue(getUdpPackage(),
                SocketConstants.DATAGRAM_RECORD);
        datagram.put(StringUtils.fromString(SocketConstants.DATAGRAM_REMOTE_PORT),
                datagramPacket.recipient().getPort());
        datagram.put(StringUtils.fromString(SocketConstants.DATAGRAM_REMOTE_HOST),
                StringUtils.fromString(datagramPacket.recipient().getHostName()));
        datagram.put(StringUtils.fromString(SocketConstants.DATAGRAM_DATA),
                ValueCreator.createArrayValue(byteContent));
        return  datagram;
    }

    private BArray returnByteArray(DatagramPacket datagramPacket) {
        byte[] byteContent = new byte[datagramPacket.content().readableBytes()];
        datagramPacket.content().readBytes(byteContent);

        return ValueCreator.createArrayValue(byteContent);
    }
}

