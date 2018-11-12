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
import static org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryConstants.DEFAULT_TIMEOUT;
import static org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryConstants.PARAM_TIMEOUT;

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
    public void testQueryParamValue() {
        assertEquals(DEFAULT_TIMEOUT, Util.queryParamValue("xyz://localhost", PARAM_TIMEOUT, DEFAULT_TIMEOUT));
        assertEquals(15_000, Util.queryParamValue("xyz://localhost?timeout=15000", PARAM_TIMEOUT, 30_000));
    }

    @Test
    public void testInjectQueryParam() {
        assertEquals("zmq://127.0.0.1?abc=123", Util.injectQueryParam("zmq://127.0.0.1", "abc", "123"));
        assertEquals("zmq://127.0.0.1:10000?abc=123", Util.injectQueryParam("zmq://127.0.0.1:10000", "abc", "123"));
        assertEquals("zmq://127.0.0.1/path?abc=123", Util.injectQueryParam("zmq://127.0.0.1/path", "abc", "123"));
        assertEquals("zmq://127.0.0.1:10000/path?abc=123",
                Util.injectQueryParam("zmq://127.0.0.1:10000/path", "abc", "123"));
        assertEquals("zmq://127.0.0.1:10000/path?query1=1&query2=2&abc=123",
                Util.injectQueryParam("zmq://127.0.0.1:10000/path?query1=1&query2=2", "abc", "123"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInjectQueryParamInvalidUri() {
        Util.injectQueryParam("%not-a-valid-uri", "", "");
    }
}
