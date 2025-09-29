/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.messagelib.DefaultTransportFactory;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.impl.RemoteControl;
import org.opendaylight.jsonrpc.model.RemoteRpcInvoker;
import org.opendaylight.jsonrpc.test.TestErrorMethod;
import org.opendaylight.jsonrpc.test.TestFactorial;
import org.opendaylight.jsonrpc.test.TestMultiplyList;
import org.opendaylight.jsonrpc.test.TestRemoveCoffeePot;
import org.opendaylight.jsonrpc.test.TestSimpleMethod;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.mdsal.dom.broker.RouterDOMPublishNotificationService;
import org.opendaylight.mdsal.dom.broker.RouterDOMRpcProviderService;
import org.opendaylight.mdsal.dom.broker.RouterDOMRpcService;
import org.opendaylight.yangtools.binding.data.codec.impl.di.DefaultBindingDOMCodecFactory;
import org.opendaylight.yangtools.concepts.Registration;

/**
 * Test for {@link RemoteRpcInvoker} part of {@link RemoteControl}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 28, 2019
 */
public class RemoteRpcInvokerTest extends AbstractJsonRpcTest {
    private DefaultTransportFactory transportFactory;
    private Registration rpcReg;
    private RemoteControl ctrl;
    private JsonRpcCodecFactory codecFactory;

    @Before
    public void setUp() throws Exception {
        final BindingDOMRpcProviderServiceAdapter rpcAdapter = new BindingDOMRpcProviderServiceAdapter(
            new ConstantAdapterContext(new DefaultBindingDOMCodecFactory().createBindingDOMCodec(
                getBindingRuntimeContext())),
            new RouterDOMRpcProviderService(getDOMRpcRouter()));
        rpcReg = rpcAdapter.registerRpcImplementations(
            new TestErrorMethod(),
            new TestFactorial(),
            new TestMultiplyList(),
            new TestRemoveCoffeePot(),
            new TestSimpleMethod());
        testCustomizer.updateSchema(runtimeContext);
        codecFactory = new JsonRpcCodecFactory(schemaContext);
        transportFactory = new DefaultTransportFactory();
        ctrl = new RemoteControl(getDomBroker(), schemaContext, transportFactory,
            new RouterDOMPublishNotificationService(getDOMNotificationRouter()),
            new RouterDOMRpcService(getDOMRpcRouter()), codecFactory);

        logTestName("START");
    }

    @After
    public void tearDown() {
        rpcReg.close();
        transportFactory.close();
        ctrl.close();
        logTestName("END");
    }

    @Test
    public void testInvokeWithModulePrefix() {
        JsonElement result = ctrl.invokeRpc("test-model-rpc:simple-method", new JsonObject());
        assertNotNull(result);
    }

    @Test
    public void testInvokeWithoutModulePrefix() {
        JsonElement result = ctrl.invokeRpc("simple-method", new JsonObject());
        assertNotNull(result);
    }

    @Test
    public void testInvokeNonExistentMethod() {
        assertThrows(IllegalArgumentException.class,
            () -> ctrl.invokeRpc("test-model:method-not-exists", new JsonObject()));
    }

    @Test
    public void testInvokeNonExistentPrefix() {
        assertThrows(IllegalArgumentException.class,
            () -> ctrl.invokeRpc("module-not-exists:simple-method", new JsonObject()));
    }
}
