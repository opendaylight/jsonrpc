/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.Uninterruptibles;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
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
        factory.endpointBuilder().responder().create("xyz://0.0.0.0:11111", (AutoCloseable) () -> {
            // NOOP
        }).close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyTransport() throws URISyntaxException {
        factory.endpointBuilder().responder().create("", (AutoCloseable) () -> {
            // NOOP
        }).close();
    }

    @Test(timeout = 15_000)
    public void testIsClientConnected() throws Exception {
        final int port = TestHelper.getFreeTcpPort();
        final SomeService proxy = factory.endpointBuilder()
                .requester()
                .useCache()
                .withProxyConfig(10, 150)
                .withRequestTimeout(10_000)
                .createProxy(SomeService.class, TestHelper.getConnectUri("ws", port));
        final ResponderSession responder = factory.endpointBuilder()
                .responder()
                .useCache()
                .create(TestHelper.getBindUri("ws", port), mock(SomeService.class));
        for (int i = 0; i < 10; i++) {
            if (factory.isClientConnected(proxy)) {
                break;
            }
            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
        }
        assertTrue(factory.isClientConnected(proxy));
        responder.close();
        proxy.close();
    }

    @After
    public void tearDown() throws Exception {
        factory.close();
    }
}
