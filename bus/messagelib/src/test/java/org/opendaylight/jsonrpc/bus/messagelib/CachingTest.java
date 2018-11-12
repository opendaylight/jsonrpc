/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage.Builder;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;

public class CachingTest {
    private MessageLibrary ml;
    private static final RequestMessageHandler MOCK_REQUEST_HANDLER = new RequestMessageHandler() {
        @Override
        public void handleRequest(JsonRpcRequestMessage request, Builder replyBuilder) {
            // NOOP
        }
    };
    private static final ReplyMessageHandler MOCK_REPLY_HANDLER = new ReplyMessageHandler() {
        @Override
        public void handleReply(JsonRpcReplyMessage reply) {
            // NOOP
        }
    };

    @Before
    public void setUp() {
        ml = new MessageLibrary("ws");
    }

    @After
    public void tearDown() {
        ml.close();
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Test
    public void test() {
        int port = TestHelper.getFreeTcpPort();
        ResponderSession responder = ml.responder(TestHelper.getBindUri("ws", port), MOCK_REQUEST_HANDLER);
        RequesterSession requester1 = ml.requester(TestHelper.getConnectUri("ws", port), MOCK_REPLY_HANDLER);
        RequesterSession requester2 = ml.requester(TestHelper.getConnectUri("ws", port), MOCK_REPLY_HANDLER);
        RequesterSession requester3 = ml.requester(TestHelper.getConnectUri("ws", port), MOCK_REPLY_HANDLER);
        // ensure that cached instance is returned instead
        assertEquals(requester1, requester2);
        assertEquals(requester1, requester3);
        // 2 sessions : 1xREQ + 1xREP
        assertEquals(2, ml.getSessionCount());
        requester1.close();
        assertEquals(2, ml.getSessionCount());
        requester2.close();
        assertEquals(2, ml.getSessionCount());
        requester3.close();
        // only responder remaining
        assertEquals(1, ml.getSessionCount());
        responder.close();
        // no sessions
        assertEquals(0, ml.getSessionCount());
    }
}
