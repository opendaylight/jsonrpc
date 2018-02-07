/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
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

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.BusSession;
import org.opendaylight.jsonrpc.bus.SessionType;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;

public class ThreadedSesssionImplTest {
    private static class TestHandler implements AutoCloseable {

        @SuppressWarnings("unused")
        public String concat(int val, String str) {
            return "" + val + str;
        }

        @Override
        public void close() throws Exception {
        }
    }

    private final MessageLibrary mockMessaging = mock(MessageLibrary.class);
    private final BusSession mockBusSession = mock(BusSession.class);
    private final TestHandler handler = new TestHandler();

    private ThreadedSessionImpl<TestHandler> instance;

    private final JsonRpcRequestMessage.Builder requestBuilder = JsonRpcRequestMessage.builder().idFromIntValue(ANYID);
    private final JsonRpcReplyMessage.Builder replyBuilder = JsonRpcReplyMessage.builder().idFromIntValue(ANYID);

    private static final String TARGET_METHOD = "concat"; // from TestHandler class above
    private static final int ANYID = 1;

    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INVALID_PARAMS   = -32602;

    @Before
    public void setup() {
        when(mockBusSession.getSessionType()).thenReturn(SessionType.RESPONDER);

        instance = new ThreadedSessionImpl<>(mockMessaging, mockBusSession, handler);
    }

    @Test
    public void canSuccessfullyInvokeTargetMethod() {
        // given
        requestBuilder.method(TARGET_METHOD).paramsFromObject(new Object[] { 1, "abc" });
        // when
        instance.handleRequest(requestBuilder.build(), replyBuilder);
        // then
        assertEquals("1abc", replyBuilder.build().getResult().getAsString());
    }

    @Test
    public void canSuccessfullyCoerceParameters() {
        // given
        requestBuilder.method(TARGET_METHOD).paramsFromObject(new Object[] { "1", "abc" });
        // when
        instance.handleRequest(requestBuilder.build(), replyBuilder);
        // then
        assertEquals("1abc", replyBuilder.build().getResult().getAsString());
    }

    @Test
    public void invalidMethodCallIsNoticed() {
        // given
        requestBuilder.method("foobar");
        // when
        instance.handleRequest(requestBuilder.build(), replyBuilder);
        // then
        JsonRpcReplyMessage reply = replyBuilder.build();
        assertTrue(reply.isError());
        assertEquals(METHOD_NOT_FOUND, reply.getError().getCode());
    }

    @Test
    public void invalidParameterCountIsNoticed() {
        // given
        requestBuilder.method(TARGET_METHOD).paramsFromObject(new Object[] { 1 });
        // when
        instance.handleRequest(requestBuilder.build(), replyBuilder);
        // then
        JsonRpcReplyMessage reply = replyBuilder.build();
        assertTrue(reply.isError());
        assertEquals(INVALID_PARAMS, reply.getError().getCode());
    }

    @Test
    public void invalidParameterTypeIsNoticed() {
        // given
        String notConvertableToInt = "x";
        requestBuilder.method(TARGET_METHOD).paramsFromObject(new Object[] { notConvertableToInt, "abc" });
        // when
        instance.handleRequest(requestBuilder.build(), replyBuilder);
        // then
        JsonRpcReplyMessage reply = replyBuilder.build();
        assertTrue(reply.isError());
        assertEquals(INVALID_PARAMS, reply.getError().getCode());
    }
}
