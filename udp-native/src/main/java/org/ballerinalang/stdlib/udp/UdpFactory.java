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
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;

/**
 * {@link UdpFactory} creates {@link UdpClient} and UdpListener.
 */
public class UdpFactory {

    private static volatile UdpFactory udpFactory;
    private EventLoopGroup group;

    private UdpFactory() {
        group = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
    }

    public static UdpFactory getInstance() {
        if (udpFactory == null) {
            udpFactory = new UdpFactory();
        }
        return udpFactory;
    }

    public UdpClient createUdpClient(InetSocketAddress localAddress, InetSocketAddress remoteAddress, Future callback) {
        return new UdpClient(localAddress, remoteAddress, getInstance().group, callback);
    }

    public UdpClient createUdpClient(InetSocketAddress localAddress, Future callback) {
        return new UdpClient(localAddress, getInstance().group, callback);
    }

    public UdpListener createUdpListener(InetSocketAddress localAddress, InetSocketAddress remoteAddress,
                                         Future callback, UdpService udpService) {
        return new UdpListener(localAddress, remoteAddress, getInstance().group, callback, udpService);
    }
}
