/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UriParserTest {
    @Test
    public void test() {
        assertTrue(UriParser.parse("ws://0.0.0.0/?auth").containsKey("auth"));
        assertEquals("1", UriParser.parse("ws://0.0.0.0/?auth=1").get("auth"));
    }
}
