/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.mdsal.dom.broker.DOMRpcRouter;
import org.opendaylight.mdsal.dom.spi.FixedDOMSchemaService;
import org.opendaylight.yangtools.binding.Rpc;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.data.codec.impl.di.DefaultBindingDOMCodecFactory;
import org.opendaylight.yangtools.binding.data.codec.spi.BindingDOMCodecServices;
import org.opendaylight.yangtools.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.yangtools.binding.runtime.spi.BindingRuntimeHelpers;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

/**
 * Implementation of {@link RpcInvocationAdapter} that is used in embedded applications.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 24, 2018
 */
public final class EmbeddedRpcInvocationAdapter implements RpcInvocationAdapter {
    public static final EmbeddedRpcInvocationAdapter INSTANCE = new EmbeddedRpcInvocationAdapter();

    private final BindingDOMCodecServices codec;
    private final SchemaChangeAwareConverter converter;
    private final DOMRpcRouter rpcService;
    private final BindingDOMRpcProviderServiceAdapter rpcAdapter;

    private EmbeddedRpcInvocationAdapter() {
        final var runtimeContext = BindingRuntimeHelpers.createRuntimeContext();
        codec = new DefaultBindingDOMCodecFactory().createBindingDOMCodec(runtimeContext);
        final var schemaService = new FixedDOMSchemaService(runtimeContext.modelContext());
        converter = new SchemaChangeAwareConverter(schemaService);
        rpcService = new DOMRpcRouter(schemaService);
        rpcAdapter = new BindingDOMRpcProviderServiceAdapter(new ConstantAdapterContext(codec),
                rpcService.rpcProviderService());
    }

    @Override
    public SchemaChangeAwareConverter converter() {
        return converter;
    }

    @Override
    public BindingNormalizedNodeSerializer codec() {
        return codec;
    }

    @Override
    public Registration registerImpl(Rpc<?, ?> impl) {
        return rpcAdapter.registerRpcImplementation(impl);
    }

    @Override
    public EffectiveModelContext schemaContext() {
        return getRuntimeContext().modelContext();
    }

    @Override
    public BindingRuntimeContext getRuntimeContext() {
        return codec.getRuntimeContext();
    }
}
