/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See <a href="https://jira.opendaylight.org/browse/JSONRPC-16">bug report</a>.
 *
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 4, 2018
 */
public class MethodMatchingTest {
    private static final Logger LOG = LoggerFactory.getLogger(MethodMatchingTest.class);
    private final MockHandler mockHandler = new MockHandler();
    private ResponderSession responder;
    private RequesterSession requester;
    private TransportFactory tf;

    @Before
    public void setup() throws URISyntaxException {
        tf = new DefaultTransportFactory();
        final int port = TestHelper.getFreeTcpPort();
        responder = tf.endpointBuilder().responder().create(TestHelper.getBindUri("ws", port), mockHandler);
        requester = tf.endpointBuilder().requester().create(TestHelper.getConnectUri("ws", port),
                NoopReplyMessageHandler.INSTANCE);
        requester.await();
    }

    @After
    public void tearDown() throws Exception {
        requester.close();
        responder.close();
        tf.close();
    }

    @Test
    public void test() {
        final JsonArray params = new JsonArray();
        params.add("TEST");
        params.add(1);
        params.add("ABCD");
        JsonRpcReplyMessage response = requester.sendRequestAndReadReply("match_test", params);

        LOG.info("Response : {}", response);
        assertTrue(mockHandler.getCount() > 0);
    }

    @Test
    public void testInvokeFail() {
        final JsonArray params = new JsonArray();
        params.add(5);
        params.add("ABCD");
        JsonRpcReplyMessage response = requester.sendRequestAndReadReply("match_test2", params);

        LOG.info("Response : {}", response);
        assertEquals(response.getError().getCode(), -32000);
    }
}
