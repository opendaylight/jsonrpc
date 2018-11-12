/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage.Builder;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for REQ/REP pattern. As of now, all transports (http, ws, zmq) support this.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 27, 2018
 */
@RunWith(Parameterized.class)
public class ReqRepTest {
    private static final Logger LOG = LoggerFactory.getLogger(ReqRepTest.class);
    private MessageLibrary ml;
    private final String transport;

    @Parameters
    public static Collection<String[]> data() {
        return Lists.newArrayList(new String[] { "http" }, new String[] { "ws" }, new String[] { "zmq" });
    }

    public ReqRepTest(final String transport) {
        this.transport = transport;
    }

    @Before
    public void setUp() {
        ml = new MessageLibrary(transport);
    }

    @After
    public void tearDown() {
        ml.close();
    }

    @Test
    public void test() throws Exception {
        final int count = 50;
        final int port = TestHelper.getFreeTcpPort();
        final CountDownLatch replyCounter = new CountDownLatch(count);
        final CountDownLatch requestCounter = new CountDownLatch(count);
        final RequesterSession req = ml.requester(TestHelper.getConnectUri(transport, port), new ReplyMessageHandler() {
            @Override
            public void handleReply(JsonRpcReplyMessage reply) {
                LOG.info("Response received : {}", reply);
                replyCounter.countDown();
            }
        }, true);
        final ResponderSession rep = ml.responder(TestHelper.getBindUri(transport, port), new RequestMessageHandler() {
            @Override
            public void handleRequest(JsonRpcRequestMessage request, Builder replyBuilder) {
                LOG.info("Request received : {}", request);
                replyBuilder.metadata(request.getMetadata()).result(request.getParams());
                requestCounter.countDown();
            }
        }, true);
        req.await();
        for (int i = 0; i < count; i++) {
            req.sendRequest("test", null, null);
            req.read();
        }

        replyCounter.await();
        requestCounter.await();

        LOG.info("Latch1 : {}, latch2 : {}", replyCounter.getCount(), requestCounter.getCount());

        req.close();
        rep.close();
    }
}
