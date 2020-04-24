/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public final class TestHelper {
    private TestHelper() {
    }

    public static int getFreeTcpPort() {
        int port = -1;
        try {
            Socket socket = new Socket();
            socket.bind(null);
            port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String getConnectUri(String transport, int port) {
        return getUri(transport, "127.0.0.1", port);
    }

    public static String getBindUri(String transport, int port) {
        return getUri(transport, "0.0.0.0", port);
    }

    private static String getUri(String transport, String ip, int port) {
        return String.format("%s://%s:%d", transport, ip, port);
    }

    public static void awaitForProxy(TransportFactory factory, AutoCloseable proxy, long milliseconds) {
        final long future = System.currentTimeMillis() + milliseconds;
        while (System.currentTimeMillis() < future) {
            if (factory.isClientConnected(proxy)) {
                return;
            }
            Uninterruptibles.sleepUninterruptibly(100L, TimeUnit.MILLISECONDS);
        }
    }
}
