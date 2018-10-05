/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Test;

/**
 * Tests for {@link Util} class.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public class UtilTest {
    @Test
    public void testUriTokenizer() {
        Map<String, String> map = Util.UriTokenizer.tokenize("param1=a&param2=value&param3=XYZ&param4&param5=");
        assertEquals(5, map.size());
        assertEquals("a", map.get("param1"));
        assertEquals("value", map.get("param2"));
        assertNull(map.get("param4"));
        assertNull(map.get("param5"));
    }

    @Test
    public void testTimeoutInUri() {
        assertEquals(Util.DEFAULT_TIMEOUT, Util.timeoutFromUri("xyz://localhost"));
        assertEquals(15_000, Util.timeoutFromUri("xyz://localhost?timeout=15000"));
    }
}
