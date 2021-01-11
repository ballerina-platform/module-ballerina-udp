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
package org.ballerinalang.stdlib.udp.nativelistener;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.udp.Constants;
import org.ballerinalang.stdlib.udp.UdpFactory;
import org.ballerinalang.stdlib.udp.UdpListener;
import org.ballerinalang.stdlib.udp.UdpService;
import org.ballerinalang.stdlib.udp.Utils;

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Native function implementations of the UDP Listener.
 */
public class Listener {
    public static Object initEndpoint(BObject listener, int localPort, BMap<BString, Object> config) {
        listener.addNativeData(Constants.LISTENER_CONFIG, config);
        listener.addNativeData(Constants.LOCAL_PORT, localPort);
        return null;
    }

    public static Object register(Environment env, BObject listener, BObject service) {
        BMap<BString, Object> config = (BMap<BString, Object>) listener.getNativeData(Constants.LISTENER_CONFIG);
        long timeout = config.getIntValue(StringUtils.fromString(Constants.CONFIG_READ_TIMEOUT));

        listener.addNativeData(Constants.SERVICE, new UdpService(env.getRuntime(), service, timeout));
        return null;
    }

    public static Object start(Environment env, BObject listener) {
        Future balFuture = env.markAsync();

        BMap<BString, Object> config = (BMap<BString, Object>) listener.getNativeData(Constants.LISTENER_CONFIG);

        BString localHost = config.getStringValue(StringUtils.fromString(Constants.CONFIG_LOCALHOST));
        int localPort = (int) listener.getNativeData(Constants.LOCAL_PORT);
        InetSocketAddress localAddress = null;
        if (localHost == null) {
            localAddress = new InetSocketAddress(localPort);
        } else {
            String hostname = localHost.getValue();
            localAddress = new InetSocketAddress(hostname, localPort);
        }

        InetSocketAddress remoteAddress = null;
        BString remoteHost = config.getStringValue(StringUtils.fromString(Constants.CONFIG_REMOTE_HOST));
        Long remotePort = config.getIntValue(StringUtils.fromString(Constants.CONFIG_REMOTE_PORT));

        try {
            UdpService udpService = (UdpService) listener.getNativeData(Constants.SERVICE);
            remoteAddress = getRemoteAddress(remoteHost, remotePort);
            UdpListener udpListener = UdpFactory.createUdpListener(localAddress, remoteAddress, balFuture, udpService);

            listener.addNativeData(Constants.LISTENER, udpListener);
        } catch (InterruptedException e) {
            balFuture.complete(Utils.createSocketError("Unable to initialize the udp listener."));
        } catch (BindException e) {
            balFuture.complete(Utils.createSocketError("Address already in use."));
        } catch (Exception e) {
            balFuture.complete(e.getMessage());
        }

        return null;
    }

    public static Object gracefulStop(Environment env, BObject listener) {
        Future balFuture = env.markAsync();
        try {
            UdpListener udpListener = (UdpListener) listener.getNativeData(Constants.LISTENER);
            if (udpListener != null) {
                udpListener.close(balFuture);
            } else {
                balFuture.complete(Utils.createSocketError("Unable to initialize the udp listener."));
            }
        } catch (InterruptedException e) {
            balFuture.complete(Utils.createSocketError("Failed to gracefully shutdown the Listener."));
        }

        return null;
    }

    private static InetSocketAddress getRemoteAddress(BString remoteHost, Long remotePort) throws Exception {
        if (remoteHost != null && remotePort == null || remoteHost == null && remotePort != null) {
            throw new Exception("Required both remoteHost and remotePort to connect to remote address.");
        } else if (remoteHost != null && remotePort != null) {
            return new InetSocketAddress(InetAddress.getByName(remoteHost.getValue()).getHostAddress(),
                    (int) remotePort.longValue());
        }

        return null;
    }
}
