/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UtilTest {
    @Test
    public void test() {
        assertEquals("http", HttpUtil.getTransport(false, false));
        assertEquals("https", HttpUtil.getTransport(false, true));
        assertEquals("ws", HttpUtil.getTransport(true, false));
        assertEquals("wss", HttpUtil.getTransport(true, true));
    }
}
