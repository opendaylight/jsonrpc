/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import javassist.ClassPool;
import org.opendaylight.jsonrpc.impl.SchemaChangeAwareConverter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.dom.broker.DOMRpcRouter;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Implementation of {@link RpcInvocationAdapter} that is used in embedded applications.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 24, 2018
 */
public final class EmbeddedRpcInvocationAdapter implements RpcInvocationAdapter {
    private static final ClassPool CLASS_POOL = ClassPool.getDefault();
    private static final ClassLoadingStrategy CLS = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();
    private final BindingToNormalizedNodeCodec codec;
    private final EmbeddedSchemaService schemaService;
    private final SchemaChangeAwareConverter converter;
    private final DOMRpcRouter rpcService;
    private final BindingDOMRpcProviderServiceAdapter rpcAdapter;
    public static final EmbeddedRpcInvocationAdapter INSTANCE = new EmbeddedRpcInvocationAdapter();

    private EmbeddedRpcInvocationAdapter() {
        final DataObjectSerializerGenerator generator = StreamWriterGenerator
                .create(JavassistUtils.forClassPool(CLASS_POOL));
        final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(generator);
        codec = new BindingToNormalizedNodeCodec(CLS, codecRegistry);

        final ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
        moduleContext.addModuleInfos(BindingReflections.loadModuleInfos());
        final SchemaContext schemaContext = moduleContext.tryToCreateSchemaContext().get();
        schemaService = new EmbeddedSchemaService(schemaContext);
        converter = new SchemaChangeAwareConverter(schemaService);
        schemaService.registerSchemaContextListener(codec);
        rpcService = new DOMRpcRouter();
        rpcService.onGlobalContextUpdated(schemaContext);
        schemaService.registerSchemaContextListener(rpcService);
        rpcAdapter = new BindingDOMRpcProviderServiceAdapter(rpcService.getRpcProviderService(), codec);
    }

    @Override
    public SchemaChangeAwareConverter converter() {
        return converter;
    }

    @Override
    public BindingToNormalizedNodeCodec codec() {
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
}
