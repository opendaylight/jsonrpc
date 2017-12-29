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
    private JsonRpcReplyMessage reply;
    private JsonRpcRequestMessage request;

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
        reply = new JsonRpcReplyMessage();
        request = new JsonRpcRequestMessage();
        busSession = Mockito.mock(BusSession.class);
        when(busSession.getSessionType()).thenReturn(SessionType.REQUESTER);
        ts = new ThreadedSessionImpl<>(messaging, busSession, new MockHandler());
    }

    @After
    public void tearDown() {
        Mockito.reset(busSession);
        ts.joinAndClose();
    }

    /**
     * Method1 takes no args and returns void.
     */
    @Test
    public void testInvokeMethodNoParams() {
        request.setMethod("method1");
        ts.handleRequest(request, reply);
        assertNull(reply.getError());
        assertTrue(reply.getResult() instanceof JsonNull);
    }

    /**
     * Invocation of method1 with some arguments must fail, because we have
     * mismatch in number of parameters and method arguments.
     */
    @Test
    public void testInvokeMethodNoParamsButArgsProvided() {
        request.setMethod("method1");
        request.setParams(new JsonPrimitive("abc"));
        ts.handleRequest(request, reply);
        assertEquals(-32602, reply.getError().getCode());
    }

    @Test
    public void testInvokeMethodWithArgs() {
        request.setMethod("method-with-camel-case");
        request.setParams(new JsonPrimitive(8));
        ts.handleRequest(request, reply);
        assertNull(reply.getError());
        assertEquals(256, (int) reply.getResult().getAsDouble());
    }

    @Test
    public void testInvokeMethodNoParamsWithUnderscoreName() {
        request.setMethod("method-2");
        JsonArray array = new JsonArray();
        array.add(new JsonPrimitive(2));
        array.add(new JsonPrimitive("xyz"));
        request.setParams(array);
        ts.handleRequest(request, reply);
        assertNull(reply.getError());
        assertEquals("xyzxyz", reply.getResult().getAsJsonPrimitive().getAsString());
    }

    @Test
    public void testNamePreference() {
        request.setMethod("similar_method_name");
        request.setParams(new JsonPrimitive("abc"));
        ts.handleRequest(request, reply);
        assertNull(reply.getError());
        assertEquals(13, reply.getResult().getAsJsonPrimitive().getAsInt());
    }
}
