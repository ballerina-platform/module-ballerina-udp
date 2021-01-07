package org.ballerinalang.stdlib.udp;

import java.net.InetSocketAddress;

/**
 *  {@link UdpFactory} creates {@link UdpClient} and UdpListener.
 *
 */
public class UdpFactory {

    public static UdpClient createUdpClient(InetSocketAddress localAddress) throws InterruptedException {
        return new UdpClient(localAddress);
    }
}
