/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.common.util.concurrent.UncheckedExecutionException;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TransportFactoryTest {
    private TransportFactory factory;

    @Before
    public void setUp() {
        factory = new DefaultTransportFactory();
    }

    @Test(expected = UncheckedExecutionException.class)
    public void testUnknownTransport() throws URISyntaxException {
        factory.createResponder("xyz://0.0.0.0:11111", (AutoCloseable) () -> {
            // NOOP
        }).close();

    }

    @After
    public void tearDown() throws Exception {
        factory.close();
    }
}
