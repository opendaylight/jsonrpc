/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcException;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;

/**
 * Test for various method return types used in {@link ProxyService}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jan 10, 2019
 */
public class ReturnTypeTest {
    private static final InputObject OBJ1;

    static {
        OBJ1 = new InputObject();
        OBJ1.setPropertyA(10);
        OBJ1.setPropertyB(3.5f);
        OBJ1.setPropertyC("abc");
    }

    private TransportFactory tf;
    private ResponderSession responder;
    private RequesterSession requester;
    private JsonRpcReplyMessage resp;
    private GenericService proxy;

    private class GenericServiceImpl implements GenericService {

        @Override
        public void close() {
            // NOOP
        }

        @Override
        public int primitive1() {
            return 10;
        }

        @Override
        public String primitive2() {
            return "abc";
        }

        @Override
        public JsonPrimitive jsonPrimitive() {
            return new JsonPrimitive(10);
        }

        @Override
        public JsonArray jsonArray() {
            final JsonArray arr = new JsonArray();
            arr.add("abc");
            return arr;
        }

        @Override
        public JsonObject jsonObject() {
            final JsonObject ret = new JsonObject();
            ret.addProperty("abc", "123");
            return ret;
        }

        @Override
        public int[] primitiveArray() {
            return new int[] { 1, 2, 3 };
        }

        @Override
        public InputObject[] objectArray() {
            return new InputObject[] { OBJ1 };
        }

        @Override
        public List<InputObject> genericList() {
            return ImmutableList.of(OBJ1);
        }

        @Override
        public List<Map<String, InputObject>> genericMap() {
            return ImmutableList.of(ImmutableMap.<String, InputObject>builder().put("abc", OBJ1).build());
        }
    }

    @Before
    public void setUp() throws URISyntaxException, InterruptedException {
        final int port = TestHelper.getFreeTcpPort();
        tf = new DefaultTransportFactory();
        responder = tf.endpointBuilder().responder().create(TestHelper.getBindUri("zmq", port),
                new GenericServiceImpl());
        requester = tf.endpointBuilder().requester().create(TestHelper.getConnectUri("zmq", port),
                NoopReplyMessageHandler.INSTANCE);
        proxy = tf.endpointBuilder().requester().useCache().createProxy(GenericService.class,
                TestHelper.getConnectUri("zmq", port));
        TimeUnit.MILLISECONDS.sleep(100);

    }

    @After
    public void tearDown() throws Exception {
        proxy.close();
        requester.close();
        responder.close();
        tf.close();
    }

    @Test
    public void testPrimitive() throws JsonRpcException {
        resp = requester.sendRequestAndReadReply("primitive1", null);
        assertEquals(10, (int) resp.getResultAsObject(int.class));
        resp = requester.sendRequestAndReadReply("primitive2", null);
        assertEquals("abc", (String) resp.getResultAsObject(String.class));
        resp = requester.sendRequestAndReadReply("jsonPrimitive", null);
        assertEquals(10, ((JsonPrimitive) resp.getResultAsObject(JsonPrimitive.class)).getAsInt());
    }

    @Test
    public void testArray() throws JsonRpcException {
        resp = requester.sendRequestAndReadReply("jsonArray", null);
        assertEquals("abc", ((JsonArray) resp.getResultAsObject(JsonArray.class)).get(0).getAsString());
        JsonArray arr = proxy.jsonArray();
        assertEquals("abc", arr.get(0).getAsString());
        resp = requester.sendRequestAndReadReply("primitiveArray", null);
        assertEquals(1, ((JsonArray) resp.getResultAsObject(JsonArray.class)).get(0).getAsInt());
        resp = requester.sendRequestAndReadReply("objectArray", null);
        assertEquals(10, ((InputObject[]) resp.getResultAsObject(InputObject[].class))[0].getPropertyA());
        assertEquals("abc", ((InputObject[]) resp.getResultAsObject(InputObject[].class))[0].getPropertyC());
    }

    @Test
    public void testJsonObject() {
        JsonObject obj = proxy.jsonObject();
        assertEquals("123", obj.get("abc").getAsString());
    }

    @Test
    public void testGenericCollection() {
        List<Map<String, InputObject>> list1 = proxy.genericMap();
        assertEquals("abc", list1.get(0).get("abc").getPropertyC());
        List<InputObject> list2 = proxy.genericList();
        assertEquals(10, list2.get(0).getPropertyA());
    }
}
