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
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.runtime.spi.BindingRuntimeHelpers;
import org.opendaylight.mdsal.dom.broker.DOMRpcRouter;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Implementation of {@link RpcInvocationAdapter} that is used in embedded applications.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 24, 2018
 */
public final class EmbeddedRpcInvocationAdapter implements RpcInvocationAdapter {
    private final BindingCodecContext codec;
    private final EmbeddedSchemaService schemaService;
    private final SchemaChangeAwareConverter converter;
    private final DOMRpcRouter rpcService;
    private final BindingDOMRpcProviderServiceAdapter rpcAdapter;
    public static final EmbeddedRpcInvocationAdapter INSTANCE = new EmbeddedRpcInvocationAdapter();

    private EmbeddedRpcInvocationAdapter() {
        final EffectiveModelContext schemaContext = BindingRuntimeHelpers
                .createEffectiveModel(BindingRuntimeHelpers.loadModuleInfos());
        codec = new BindingCodecContext(BindingRuntimeHelpers.createRuntimeContext());
        schemaService = new EmbeddedSchemaService(schemaContext);
        converter = new SchemaChangeAwareConverter(schemaService);
        rpcService = new DOMRpcRouter();
        rpcService.onModelContextUpdated(schemaContext);
        schemaService.registerSchemaContextListener(rpcService);
        rpcAdapter = new BindingDOMRpcProviderServiceAdapter(new ConstantAdapterContext(codec),
                rpcService.getRpcProviderService());
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
    public <T extends RpcService> ObjectRegistration<T> registerImpl(Class<T> type, T impl) {
        return rpcAdapter.registerRpcImplementation(type, impl);
    }

    @Override
    public SchemaContext schemaContext() {
        return schemaService.getGlobalContext();
    }

    @Override
    public BindingRuntimeContext getRuntimeContext() {
        return codec.getRuntimeContext();
    }
}
