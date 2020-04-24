/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectParameterTest {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectParameterTest.class);
    private SomeService handler = new ServiceImpl();
    private TransportFactory tf;
    private ResponderSession responder;
    private RequesterSession requester;

    public static class ServiceImpl implements SomeService {
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
    public void setup() throws URISyntaxException, InterruptedException {
        final int port = TestHelper.getFreeTcpPort();
        tf = new DefaultTransportFactory();
        responder = tf.endpointBuilder().responder().create(TestHelper.getBindUri("zmq", port), handler);
        requester = tf.endpointBuilder().requester().create(TestHelper.getConnectUri("zmq", port),
                NoopReplyMessageHandler.INSTANCE);
        TimeUnit.MILLISECONDS.sleep(100);
    }

    @After
    public void tearDown() throws Exception {
        requester.close();
        responder.close();
        tf.close();
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
        JsonRpcReplyMessage response = requester.sendRequestAndReadReply("translate", obj);
        LOG.info("Result : {}", response.getResult().getAsString());
        assertEquals(expected, response.getResult().getAsString());
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
        JsonRpcReplyMessage response = requester.sendRequestAndReadReply("translate", obj);
        LOG.info("Result : {}", response.getResult().getAsString());
        assertEquals(expected, response.getResult().getAsString());
    }

    @Test
    public void testNegative() {
        final JsonRpcRequestMessage.Builder request = JsonRpcRequestMessage.builder();
        request.idFromIntValue(1).method("translate");
        JsonObject obj = new JsonObject();
        obj.addProperty("value", 10);
        request.params(obj);

        JsonRpcReplyMessage response = requester.sendRequestAndReadReply("translate", obj);
        assertEquals(-32000, response.getError().getCode());
    }
}
