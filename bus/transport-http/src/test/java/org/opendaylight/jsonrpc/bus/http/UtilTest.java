/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.opendaylight.jsonrpc.security.api.SecurityConstants;

public class UtilTest {
    @Test
    public void testTransportName() {
        assertEquals("http", HttpUtil.getTransport(false, false));
        assertEquals("https", HttpUtil.getTransport(false, true));
        assertEquals("ws", HttpUtil.getTransport(true, false));
        assertEquals("wss", HttpUtil.getTransport(true, true));
    }

    @Test
    public void testParseAuthHeader() {
        final String[] auth = HttpUtil
                .parseBasicAuthHeader(HttpUtil.createAuthHeader(ImmutableMap.<String, String>builder()
                        .put(SecurityConstants.OPT_USERNAME, "user123")
                        .put(SecurityConstants.OPT_PASSWORD, "pass123")
                        .build()));
        assertEquals("user123", auth[0]);
        assertEquals("pass123", auth[1]);
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingOption() {
        HttpUtil.ensureOption(ImmutableMap.<String, String>builder().build(), "missing");
    }
}
