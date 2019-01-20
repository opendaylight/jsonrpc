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
import java.util.concurrent.CountDownLatch;

import org.opendaylight.jsonrpc.binding.EmbeddedRpcInvocationAdapter;
import org.opendaylight.jsonrpc.binding.ProxyContext;
import org.opendaylight.jsonrpc.binding.SchemaAwareTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.spi.EventLoopGroupProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.bb.example.rev180924.BbExampleService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.bb.example.rev180924.Method1Input;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.bb.example.rev180924.Method1Output;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.bb.example.rev180924.Method1OutputBuilder;
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
 * This provider is meant to be running outside of ODL controller, like microservice.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 24, 2018
 */
public class OutsideController implements BbExampleService, AutoCloseable {
    private static SchemaAwareTransportFactory transport;
    private static OutsideController provider;
    private static CountDownLatch shutdownLock = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException, URISyntaxException {
        transport = new SchemaAwareTransportFactory.Builder()
                // here you can customize number of threads, etc
                .withEventLoopConfig(EventLoopGroupProvider.config())
                .withRpcInvocationAdapter(EmbeddedRpcInvocationAdapter.INSTANCE)
                .build();

        provider = new OutsideController();
        provider.init();
        shutdownLock.await();
        provider.close();
    }

    private ResponderSession responder;
    private ProxyContext<TestModelService> proxy;

    public void init() throws URISyntaxException {
        responder = transport.createResponder(BbExampleService.class, provider, "ws://0.0.0.0:20000");
        proxy = transport.createBindingRequesterProxy(TestModelService.class, "zmq://127.0.0.1:24320");
    }

    @Override
    public void close() {
        // unpublish self from bus
        responder.close();
        // close proxy
        proxy.close();
        // shutdown transport
        transport.close();
        // As this is embedded application, we might want to shutdown entire Netty machinery.
        // This operation is irreversible, so attempt to create transport after this point will fail
        EventLoopGroupProvider.shutdown();
    }

    /**
     * When this RPC method is invoked, it causes application to shutdown.
     */
    @Override
    public ListenableFuture<RpcResult<Method1Output>> method1(Method1Input input) {
        shutdownLock.countDown();
        return Futures.immediateFuture(RpcResultBuilder
                .<Method1Output>success(new Method1OutputBuilder().setOutParam(input.getInParam())).build());
    }
}
