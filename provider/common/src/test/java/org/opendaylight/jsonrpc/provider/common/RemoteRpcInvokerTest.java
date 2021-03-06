/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import static org.junit.Assert.assertNotNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.messagelib.DefaultTransportFactory;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.impl.RemoteControl;
import org.opendaylight.jsonrpc.model.RemoteRpcInvoker;
import org.opendaylight.jsonrpc.test.TestModelServiceImpl;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.TestModelRpcService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

/**
 * Test for {@link RemoteRpcInvoker} part of {@link RemoteControl}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 28, 2019
 */
public class RemoteRpcInvokerTest extends AbstractJsonRpcTest {
    private DefaultTransportFactory transportFactory;
    private ObjectRegistration<TestModelServiceImpl> rpcReg;
    private RemoteControl ctrl;
    private JsonRpcCodecFactory codecFactory;

    @Before
    public void setUp() throws Exception {
        final BindingDOMRpcProviderServiceAdapter rpcAdapter = new BindingDOMRpcProviderServiceAdapter(
                new ConstantAdapterContext(new BindingCodecContext(getBindingRuntimeContext())),
                getDOMRpcRouter().getRpcProviderService());
        rpcReg = rpcAdapter.registerRpcImplementation(TestModelRpcService.class, new TestModelServiceImpl());
        getDOMRpcRouter().onModelContextUpdated(schemaContext);
        codecFactory = new JsonRpcCodecFactory(schemaContext);
        transportFactory = new DefaultTransportFactory();
        ctrl = new RemoteControl(getDomBroker(), schemaContext, transportFactory, getDOMNotificationRouter(),
                getDOMRpcRouter().getRpcService(), codecFactory);

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

    @Test(expected = IllegalArgumentException.class)
    public void testInvokeNonExistentMethod() {
        ctrl.invokeRpc("test-model:method-not-exists", new JsonObject());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokeNonExistentPrefix() {
        ctrl.invokeRpc("module-not-exists:simple-method", new JsonObject());
    }
}
