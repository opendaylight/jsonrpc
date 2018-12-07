/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.bb.example;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import org.opendaylight.jsonrpc.binding.ControllerRpcInvocationAdapter;
import org.opendaylight.jsonrpc.binding.ProxyContext;
import org.opendaylight.jsonrpc.binding.SchemaAwareTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.spi.EventLoopConfiguration;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.bb.example.rev180924.BbExampleService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.bb.example.rev180924.Method1Input;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.bb.example.rev180924.Method1Output;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.bb.example.rev180924.Method1OutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.SimpleMethodInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rev161117.TestModelService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Example provider which shows 2 use cases for bus binding-bridge.
 * <ol>
 * <li>Create proxy against remote RPC service using jsonrpc bus</li>
 * <li>Expose self to jsonrpc bus</li>
 * </ol>
 *
 * <p>
 * This provider is meant to be running inside ODL controller.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 24, 2018
 */
public class InsideController implements BbExampleService, AutoCloseable {
    private EventLoopConfiguration eventLoopConfiguration;
    private SchemaAwareTransportFactory transport;
    private ProxyContext<TestModelService> proxy;
    private ResponderSession responder;
    private DOMSchemaService schemaService;
    private BindingToNormalizedNodeCodec codec;

    /**
     * Called by blueprint when provider is created.
     *
     * @throws URISyntaxException if URI is invalid
     */
    public void init() throws URISyntaxException {
        transport = new SchemaAwareTransportFactory.Builder().withEventLoopConfig(eventLoopConfiguration)
                .withRpcInvocationAdapter(new ControllerRpcInvocationAdapter(schemaService, codec))
                .build();
        // create proxy to remote service
        proxy = transport.createBindingRequesterProxy(TestModelService.class, "zmq://192.168.1.100:20000");
        // expose self to outside
        responder = transport.createResponder(BbExampleService.class, this, "zmq://0.0.0.0:20000");
    }

    /**
     * Called by blueprint when provider is closed.
     */
    @Override
    public void close() {
        // unpublish self from bus
        responder.close();
        // close proxy
        proxy.close();
        // shutdown transport
        transport.close();
    }

    /**
     * Invoke remote RPC method using JSONRPC.
     */
    public void invokeRemoteServiceFromController() throws InterruptedException, ExecutionException {
        proxy.getProxy().simpleMethod(new SimpleMethodInputBuilder().build()).get();
    }

    /*
     * This method can be invoked via RESTConf or via JSONRPC bus
     */
    @Override
    public ListenableFuture<RpcResult<Method1Output>> method1(Method1Input input) {
        return Futures.immediateFuture(RpcResultBuilder
                .<Method1Output>success(new Method1OutputBuilder().setOutParam(input.getInParam())).build());
    }

    /*
     * Set by blueprint
     */
    public void setEventLoopConfiguration(EventLoopConfiguration eventLoopConfiguration) {
        this.eventLoopConfiguration = eventLoopConfiguration;
    }

    public void setSchemaService(DOMSchemaService schemaService) {
        this.schemaService = schemaService;
    }

    public void setCodec(BindingToNormalizedNodeCodec codec) {
        this.codec = codec;
    }
}
