/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.BusSession;
import org.opendaylight.jsonrpc.bus.SessionType;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectParameterTest {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectParameterTest.class);
    private MessageLibrary mockMessaging = mock(MessageLibrary.class);
    private BusSession mockBusSession = mock(BusSession.class);

    private ThreadedSessionImpl<SomeService> instance;
    private SomeService handler = new ServiceImpl();

    private class ServiceImpl implements SomeService {
        @Override
        public void close() throws Exception {
            // NOOP
        }

        @Override
        public String translate(InputObject input) {
            if (input.getPropertyA() == 0) {
                throw new IllegalArgumentException();
            }
            return input.toString();
        }

        @Override
        public int translate(UninstantiableObject input) {
            return 0;
        }
    }

    @Before
    public void setup() {
        when(mockBusSession.getSessionType()).thenReturn(SessionType.RESPONDER);
        instance = new ThreadedSessionImpl<>(mockMessaging, mockBusSession, handler);
    }

    @After
    public void tearDown() throws Exception {
        instance.close();
    }

    @Test
    public void testNativeObject() {
        final JsonRpcRequestMessage.Builder request = JsonRpcRequestMessage.builder();
        request.idFromIntValue(1).method("translate");
        final NestedObject no = new NestedObject();
        no.setPropertyD("TEST2");
        final InputObject obj = new InputObject();
        obj.setPropertyA(10);
        obj.setPropertyB(45334.786f);
        obj.setPropertyC("TEST");
        obj.setPropertyD(no);
        request.paramsFromObject(obj);
        final String expected = obj.toString();

        final JsonRpcReplyMessage.Builder reply = JsonRpcReplyMessage.builder();
        instance.handleRequest(request.build(), reply);
        final JsonRpcReplyMessage result = reply.build();
        LOG.info("Result : {}", result.getResult().getAsString());
        assertEquals(expected, result.getResult().getAsString());
    }

    @Test
    public void testJsonObject() {
        final JsonRpcRequestMessage.Builder request = JsonRpcRequestMessage.builder();
        request.idFromIntValue(1).method("translate");
        final NestedObject no = new NestedObject();
        no.setPropertyD("TEST2");
        final InputObject obj = new InputObject();
        obj.setPropertyA(10);
        obj.setPropertyB(45334.786f);
        obj.setPropertyC("TEST");
        obj.setPropertyD(no);
        request.params(new Gson().toJsonTree(obj));
        final String expected = obj.toString();

        JsonRpcReplyMessage.Builder reply = JsonRpcReplyMessage.builder();
        instance.handleRequest(request.build(), reply);

        final JsonRpcReplyMessage result = reply.build();
        LOG.info("Result : {}", result.getResult().getAsString());
        assertEquals(expected, result.getResult().getAsString());
    }

    @Test
    public void testNegative() {
        final JsonRpcRequestMessage.Builder request = JsonRpcRequestMessage.builder();
        request.idFromIntValue(1).method("translate");
        JsonObject obj = new JsonObject();
        obj.addProperty("value", 10);
        request.params(obj);

        JsonRpcReplyMessage.Builder reply = JsonRpcReplyMessage.builder();
        instance.handleRequest(request.build(), reply);
        LOG.info("{}", reply);
        final JsonRpcReplyMessage result = reply.build();
        assertEquals(-32000, result.getError().getCode());
    }
}
