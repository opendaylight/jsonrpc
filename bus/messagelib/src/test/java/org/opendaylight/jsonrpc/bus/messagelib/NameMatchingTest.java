/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.jsonrpc.bus.BusSession;
import org.opendaylight.jsonrpc.bus.SessionType;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;

public class NameMatchingTest {
    private static MessageLibrary messaging;
    private BusSession busSession;
    private ThreadedSessionImpl<AutoCloseable> ts;
    private final JsonRpcReplyMessage.Builder replyBuilder = JsonRpcReplyMessage.builder();
    private final JsonRpcRequestMessage.Builder requestBuilder = JsonRpcRequestMessage.builder();

    @BeforeClass
    public static void setupBeforeClass() {
        messaging = new MessageLibrary("zmq");
    }

    @AfterClass
    public static void tearDownAfterClass() {
        messaging.close();
    }

    @Before
    public void setUp() {
        busSession = Mockito.mock(BusSession.class);
        when(busSession.getSessionType()).thenReturn(SessionType.REQUESTER);
        ts = new ThreadedSessionImpl<>(messaging, busSession, new MockHandler());
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException, TimeoutException {
        Mockito.reset(busSession);
        ts.stop().get(10, TimeUnit.SECONDS);
    }

    /**
     * Method1 takes no args and returns void.
     */
    @Test
    public void testInvokeMethodNoParams() {
        requestBuilder.method("method1");
        ts.handleRequest(requestBuilder.build(), replyBuilder);
        JsonRpcReplyMessage reply = replyBuilder.build();
        assertNull(reply.getError());
        assertTrue(reply.getResult() instanceof JsonNull);
    }

    /**
     * Invocation of method1 with some arguments must fail, because we have
     * mismatch in number of parameters and method arguments.
     */
    @Test
    public void testInvokeMethodNoParamsButArgsProvided() {
        requestBuilder.method("method1").params(new JsonPrimitive("abc"));
        ts.handleRequest(requestBuilder.build(), replyBuilder);
        assertEquals(-32602, replyBuilder.build().getError().getCode());
    }

    @Test
    public void testInvokeMethodWithArgs() {
        requestBuilder.method("method-with-camel-case").params(new JsonPrimitive(8));
        ts.handleRequest(requestBuilder.build(), replyBuilder);
        JsonRpcReplyMessage reply = replyBuilder.build();
        assertNull(reply.getError());
        assertEquals(256, (int) reply.getResult().getAsDouble());
    }

    @Test
    public void testInvokeMethodNoParamsWithUnderscoreName() {
        requestBuilder.method("method-2");
        JsonArray array = new JsonArray();
        array.add(new JsonPrimitive(2));
        array.add(new JsonPrimitive("xyz"));
        requestBuilder.params(array);
        ts.handleRequest(requestBuilder.build(), replyBuilder);
        JsonRpcReplyMessage reply = replyBuilder.build();
        assertNull(reply.getError());
        assertEquals("xyzxyz", reply.getResult().getAsJsonPrimitive().getAsString());
    }

    @Test
    public void testNamePreference() {
        requestBuilder.method("similar_method_name").params(new JsonPrimitive("abc"));
        ts.handleRequest(requestBuilder.build(), replyBuilder);
        JsonRpcReplyMessage reply = replyBuilder.build();
        assertNull(reply.getError());
        assertEquals(13, reply.getResult().getAsJsonPrimitive().getAsInt());
    }
}
