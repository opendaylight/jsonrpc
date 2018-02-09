/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.api.SessionType;

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
    public void testReplaceUri() {
        assertEquals("role=REQ", Util.replaceParam("role=REP", "role", SessionType.REQ.name()));
        assertEquals("role=REP", Util.replaceParam("", "role", SessionType.REP.name()));
    }

    @Test
    public void testPrepareUri() throws Exception {
        String uri = Util.prepareUri(new URI("zmq://localhost:5432/?param1=value1&param2=value2&role=REP"));
        assertFalse(Util.UriTokenizer.tokenize(uri).containsKey("role"));
    }
}
