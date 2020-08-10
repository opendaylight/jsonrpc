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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.messagelib.NoopReplyMessageHandler;
import org.opendaylight.jsonrpc.bus.messagelib.RequesterSession;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.TestHelper;
import org.opendaylight.jsonrpc.test.TestModelServiceImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.base.rev201014.numbers.list.Numbers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.base.rev201014.numbers.list.NumbersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.Coffee;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.ErrorMethodInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.ErrorMethodOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.FactorialInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.FactorialOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.MultiplyListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.RemoveCoffeePotInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.RemoveCoffeePotOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.SimpleMethodInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.TestModelRpcService;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcResult;
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
    private ProxyContext<TestModelRpcService> proxy;
    private RequesterSession requester;

    @Before
    public void setUp() throws Exception {
        transportFactory = new SchemaAwareTransportFactory.Builder()
                .withRpcInvocationAdapter(EmbeddedRpcInvocationAdapter.INSTANCE).build();
        final int port = TestHelper.getFreeTcpPort();
        responder = transportFactory.createResponder(TestModelRpcService.class, new TestModelServiceImpl(),
                TestHelper.getBindUri("ws", port));
        proxy = transportFactory.createBindingRequesterProxy(TestModelRpcService.class,
                TestHelper.getConnectUri("ws", port));
        requester = transportFactory.createRequester(TestHelper.getConnectUri("ws", port),
                NoopReplyMessageHandler.INSTANCE);
        TimeUnit.MILLISECONDS.sleep(150);
    }

    @After
    public void tearDown() {
        responder.close();
        proxy.close();
        transportFactory.close();
    }

    @Test
    public void testFactorial() throws Exception {
        final Future<RpcResult<FactorialOutput>> result = proxy.getProxy()
                .factorial(new FactorialInputBuilder().setInNumber(Uint16.valueOf(6)).build());
        assertEquals(720L, (long) result.get().getResult().getOutNumber().longValue());
    }

    @Test
    public void testMultiplyList() throws Exception {
        final Collection<Numbers> resp = proxy.getProxy()
                .multiplyList(new MultiplyListInputBuilder().setMultiplier((short) 10)
                        .setNumbers(Maps.uniqueIndex(Lists.newArrayList(new NumbersBuilder().setNum(10).build(),
                                new NumbersBuilder().setNum(20).build()), Identifiable::key))
                        .build())
                .get()
                .getResult()
                .getNumbers().values();

        assertEquals(2, resp.size());
    }

    @Test
    public void testErrorMethod() throws Exception {
        final RpcResult<ErrorMethodOutput> result = proxy.getProxy()
                .errorMethod(new ErrorMethodInputBuilder().build())
                .get();
        assertFalse(result.isSuccessful());
        final RpcError err = result.getErrors().iterator().next();
        assertEquals("Ha!", err.getMessage());
        assertEquals(ErrorSeverity.ERROR, err.getSeverity());
    }

    @Test
    public void testSimpleMethod() throws Exception {
        assertTrue(proxy.getProxy().simpleMethod(new SimpleMethodInputBuilder().build()).get().isSuccessful());
    }

    @Test
    public void testSimpleMethodNull() {
        JsonRpcReplyMessage reply = requester.sendRequestAndReadReply("simple-method", null);
        assertEquals(null, reply.getError());
    }

    @Test
    public void testRemoveCoffeePot() throws Exception {
        RemoveCoffeePotOutput result = proxy.getProxy()
                .removeCoffeePot(new RemoveCoffeePotInputBuilder().build())
                .get()
                .getResult();
        assertEquals(result.getCupsBrewed().longValue(), (long) 6);
        assertEquals(result.getDrink(), Coffee.class);
    }

    @Test
    public void testSendPrimitiveParameter() {
        final JsonRpcReplyMessage response = requester.sendRequestAndReadReply("factorial", 5);
        assertEquals(120, response.getResult().getAsJsonObject().get("out-number").getAsInt());
    }

    @Test
    public void testSendSingleArrayParameter() {
        final JsonRpcReplyMessage response = requester.sendRequestAndReadReply("factorial", new Object[] { 5 });
        assertEquals(120, response.getResult().getAsJsonObject().get("out-number").getAsInt());
    }

    @Test
    public void testInvalidMethod() {
        final JsonRpcReplyMessage response = requester.sendRequestAndReadReply(UUID.randomUUID().toString(), null);
        assertEquals(-32601, response.getError().getCode());
    }
}
