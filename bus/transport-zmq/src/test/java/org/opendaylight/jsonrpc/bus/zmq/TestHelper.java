/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import java.io.IOException;

public class TestHelper {
    public static String getFreeTcpPort() {
        int port = -1;
        try {
            java.net.Socket s = new java.net.Socket();
            s.bind(null);
            port = s.getLocalPort();
            s.close();
            return Integer.toString(port);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
