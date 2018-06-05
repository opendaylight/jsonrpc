/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UriBuilderTest {
    @Test
    public void test() {
        assertEquals("http://localhost?auth=1&xyz=3",
                new UriBuilder("http://localhost").add("auth", "1").add("xyz", "3").build());
    }
}
