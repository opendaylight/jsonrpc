/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;

import com.google.common.util.concurrent.Uninterruptibles;

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test to verify <a href="https://jira.opendaylight.org/browse/JSONRPC-30">JSONRPC-30</a>.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since May 26, 2019
 */
public class JsonRrpc30Test {
    private static final long DELAY = 500;
    private DefaultTransportFactory tf;
    private ResponderSession responder;
    private TestApi requester;

    public interface TestApi extends AutoCloseable {
        String test(String data);

        @Override
        default void close() {
            // NOOP
        }
    }

    public class TestImpl implements TestApi {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public String test(String data) {
            if (counter.incrementAndGet() == 1) {
                // first request
                Uninterruptibles.sleepUninterruptibly(DELAY, TimeUnit.MILLISECONDS);
            }
            return data;
        }

    }

    @Before
    public void setUp() throws URISyntaxException {
        tf = new DefaultTransportFactory();
        final int port = TestHelper.getFreeTcpPort();
        final String uri = TestHelper.getConnectUri("zmq", port) + "?timeout=300";

        responder = tf.createResponder(uri, new TestImpl());
        requester = tf.createRequesterProxy(TestApi.class, uri);
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
    }

    @After
    public void tearDown() {
        requester.close();
        responder.close();
        tf.close();
    }

    @Test
    public void test() {
        try {
            requester.test("abcd");
        } catch (MessageLibraryException e) {
            // ignore, this is expected
        }
        Uninterruptibles.sleepUninterruptibly(DELAY * 2, TimeUnit.MILLISECONDS);

        assertEquals("xyz", requester.test("xyz"));
    }
}
