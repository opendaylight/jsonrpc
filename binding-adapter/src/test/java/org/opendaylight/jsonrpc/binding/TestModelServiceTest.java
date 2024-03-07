/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.messagelib.NoopReplyMessageHandler;
import org.opendaylight.jsonrpc.bus.messagelib.RequesterSession;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.TestHelper;
import org.opendaylight.jsonrpc.test.TestErrorMethod;
import org.opendaylight.jsonrpc.test.TestFactorial;
import org.opendaylight.jsonrpc.test.TestMultiplyList;
import org.opendaylight.jsonrpc.test.TestRemoveCoffeePot;
import org.opendaylight.jsonrpc.test.TestSimpleMethod;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.base.rev201014.numbers.list.NumbersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.Coffee;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.ErrorMethod;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.ErrorMethodInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.Factorial;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.FactorialInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.MultiplyList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.MultiplyListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.RemoveCoffeePot;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.RemoveCoffeePotInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.SimpleMethod;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.SimpleMethodInputBuilder;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.Uint16;

/**
 * Test various methods implemented by {@link TestModelServiceImpl}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 21, 2018
 */
public class TestModelServiceTest {
    private SchemaAwareTransportFactory transportFactory;
    private ResponderSession responder;
    private RequesterSession requester;
    private String connectUri;

    @Before
    public void setUp() throws Exception {
        transportFactory = new SchemaAwareTransportFactory.Builder()
                .withRpcInvocationAdapter(EmbeddedRpcInvocationAdapter.INSTANCE).build();
        final int port = TestHelper.getFreeTcpPort();
        connectUri = TestHelper.getConnectUri("ws", port);
        responder = transportFactory.createMultiModelResponder(MultiModelBuilder.create()
            .addService(new TestErrorMethod())
            .addService(new TestFactorial())
            .addService(new TestMultiplyList())
            .addService(new TestRemoveCoffeePot())
            .addService(new TestSimpleMethod()), TestHelper.getBindUri("ws", port));

        requester = transportFactory.endpointBuilder().requester().create(TestHelper.getConnectUri("ws", port),
                NoopReplyMessageHandler.INSTANCE);
        TimeUnit.MILLISECONDS.sleep(150);
    }

    @After
    public void tearDown() {
        responder.close();
        transportFactory.close();
    }

    @Test
    public void testFactorial() throws Exception {
        try (var proxy = transportFactory.createBindingRequesterProxy(Factorial.class, connectUri)) {
            final var result = proxy.getProxy().invoke(new FactorialInputBuilder()
                .setInNumber(Uint16.valueOf(6))
                .build());
            assertEquals(720L, result.get().getResult().getOutNumber().longValue());
        }
    }

    @Test
    public void testMultiplyList() throws Exception {
        try (var proxy = transportFactory.createBindingRequesterProxy(MultiplyList.class, connectUri)) {
            final var resp = proxy.getProxy().invoke(new MultiplyListInputBuilder()
                .setMultiplier((short) 10)
                .setNumbers(BindingMap.ordered(
                    new NumbersBuilder().setNum(10).build(),
                    new NumbersBuilder().setNum(20).build()))
                .build())
                .get()
                .getResult()
                .nonnullNumbers()
                .values();

            assertEquals(2, resp.size());
        }
    }

    @Test
    public void testErrorMethod() throws Exception {
        try (var proxy = transportFactory.createBindingRequesterProxy(ErrorMethod.class, connectUri)) {
            final var result = proxy.getProxy().invoke(new ErrorMethodInputBuilder().build()).get();
            assertFalse(result.isSuccessful());
            final var err = result.getErrors().iterator().next();
            assertEquals("Ha!", err.getMessage());
            assertEquals(ErrorSeverity.ERROR, err.getSeverity());
        }
    }

    @Test
    public void testSimpleMethod() throws Exception {
        try (var proxy = transportFactory.createBindingRequesterProxy(SimpleMethod.class, connectUri)) {
            assertTrue(proxy.getProxy().invoke(new SimpleMethodInputBuilder().build()).get().isSuccessful());
        }
    }

    @Test
    public void testSimpleMethodNull() {
        final var reply = requester.sendRequestAndReadReply("simple-method", null);
        assertEquals(null, reply.getError());
    }

    @Test
    public void testRemoveCoffeePot() throws Exception {
        try (var proxy = transportFactory.createBindingRequesterProxy(RemoveCoffeePot.class, connectUri)) {
            final var result = proxy.getProxy().invoke(new RemoveCoffeePotInputBuilder().build())
                .get()
                .getResult();
            assertEquals(6, result.getCupsBrewed().longValue());
            assertEquals(Coffee.VALUE, result.getDrink());
        }
    }

    @Test
    public void testSendPrimitiveParameter() {
        final var response = requester.sendRequestAndReadReply("factorial", 5);
        assertEquals(120, response.getResult().getAsJsonObject().get("out-number").getAsInt());
    }

    @Test
    public void testSendSingleArrayParameter() {
        final var response = requester.sendRequestAndReadReply("factorial", new Object[] { 5 });
        assertEquals(120, response.getResult().getAsJsonObject().get("out-number").getAsInt());
    }

    @Test
    public void testInvalidMethod() {
        final var response = requester.sendRequestAndReadReply(UUID.randomUUID().toString(), null);
        assertEquals(-32601, response.getError().getCode());
    }
}
