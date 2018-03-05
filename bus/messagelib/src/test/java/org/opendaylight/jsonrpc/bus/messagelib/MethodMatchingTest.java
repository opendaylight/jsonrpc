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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.BusSession;
import org.opendaylight.jsonrpc.bus.SessionType;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
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
    private final MessageLibrary mockMessaging = mock(MessageLibrary.class);
    private final BusSession mockBusSession = mock(BusSession.class);
    private ThreadedSessionImpl<MockHandler> responder;
    private final MockHandler mockHandler = new MockHandler();

    @Before
    public void setup() {
        when(mockBusSession.getSessionType()).thenReturn(SessionType.RESPONDER);
        // when(mockHandler.match_test(arg1, arg2, arg3))
        responder = new ThreadedSessionImpl<>(mockMessaging, mockBusSession, mockHandler);
    }

    @Test
    public void test() {
        final JsonArray params = new JsonArray();
        params.add("TEST");
        params.add(1);
        params.add("ABCD");
        JsonRpcRequestMessage request = JsonRpcRequestMessage.builder()
                .idFromIntValue(2)
                .method("match_test")
                .params(params)
                .build();
        JsonRpcReplyMessage response;
        JsonRpcReplyMessage.Builder repBuilder = JsonRpcReplyMessage.builder();

        responder.handleRequest(request, repBuilder);

        response = repBuilder.build();

        LOG.info("Response : {}", response);
        assertTrue(mockHandler.getCount() > 0);
    }

    @Test
    public void testInvokeFail() {
        final JsonArray params = new JsonArray();
        params.add(5);
        params.add("ABCD");
        JsonRpcRequestMessage request = JsonRpcRequestMessage.builder()
                .idFromIntValue(2)
                .method("match_test2")
                .params(params)
                .build();
        JsonRpcReplyMessage response;
        JsonRpcReplyMessage.Builder repBuilder = JsonRpcReplyMessage.builder();

        responder.handleRequest(request, repBuilder);
        response = repBuilder.build();
        LOG.info("Response : {}", response);
        assertEquals(response.getError().getCode(), -32000);
    }
}
