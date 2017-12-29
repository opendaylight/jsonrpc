/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import java.io.IOException;
import java.net.Socket;

public final class TestHelper {
    private TestHelper() {
    }

    public static String getFreeTcpPort() {
        int port = -1;
        try {
            Socket socket = new Socket();
            socket.bind(null);
            port = socket.getLocalPort();
            socket.close();
            return Integer.toString(port);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
