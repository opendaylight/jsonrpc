/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import org.opendaylight.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.binding.runtime.api.BindingRuntimeTypes;
import org.opendaylight.binding.runtime.api.ClassLoadingStrategy;
import org.opendaylight.binding.runtime.api.DefaultBindingRuntimeContext;
import org.opendaylight.binding.runtime.spi.GeneratedClassLoadingStrategy;
import org.opendaylight.binding.runtime.spi.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.mdsal.binding.generator.impl.DefaultBindingRuntimeGenerator;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
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
    private static final ClassLoadingStrategy CLS = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();
    private final BindingNormalizedNodeSerializer codec;
    private final EmbeddedSchemaService schemaService;
    private final SchemaChangeAwareConverter converter;
    private final DOMRpcRouter rpcService;
    private final BindingRuntimeContext runtimeContext;
    private final BindingDOMRpcProviderServiceAdapter rpcAdapter;
    public static final EmbeddedRpcInvocationAdapter INSTANCE = new EmbeddedRpcInvocationAdapter();

    private EmbeddedRpcInvocationAdapter() {
        final ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create(CLS);
        moduleContext.registerModuleInfos(BindingReflections.loadModuleInfos());
        final EffectiveModelContext schemaContext = moduleContext.tryToCreateModelContext()
                .orElseThrow(() -> new IllegalStateException("Failed to create SchemaContext"));

        final DefaultBindingRuntimeGenerator rtg = new DefaultBindingRuntimeGenerator();
        final BindingRuntimeTypes runtimeTypes = rtg.generateTypeMapping(schemaContext);
        runtimeContext = DefaultBindingRuntimeContext.create(runtimeTypes, CLS);
        codec = new BindingCodecContext(runtimeContext);

        schemaService = new EmbeddedSchemaService(schemaContext);
        converter = new SchemaChangeAwareConverter(schemaService);
        rpcService = new DOMRpcRouter();
        rpcService.onModelContextUpdated(schemaContext);
        schemaService.registerSchemaContextListener(rpcService);
        rpcAdapter = new BindingDOMRpcProviderServiceAdapter(
                new ConstantAdapterContext(new BindingCodecContext(runtimeContext)),
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
        return runtimeContext;
    }
}
