/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.reflect.AbstractInvocationHandler;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Objects;
import java.util.stream.Collector;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.spec.naming.BindingMapping;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

/**
 * Abstract class that hold common fields for in/out handlers.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 20, 2018
 */
abstract class AbstractHandler<T extends RpcService> extends AbstractInvocationHandler {
    protected final BiMap<Method, RpcDefinition> rpcMethodMap;
    protected final RpcInvocationAdapter adapter;

    AbstractHandler(final Class<T> type, RpcInvocationAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter);
        rpcMethodMap = getRpcMethodToSchemaPath(type).entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(),
                        adapter.schemaContext()
                                .getOperations()
                                .stream()
                                .filter(r -> r.getQName().equals(e.getValue()))
                                .findFirst()
                                .orElseThrow()))
                .collect(Collector.of(ImmutableBiMap::<Method, RpcDefinition>builder,
                    (builder, entry) -> builder.put(entry.getKey(), entry.getValue()),
                    (k, v) -> k.putAll(v.build()), ImmutableBiMap.Builder<Method, RpcDefinition>::build));
    }

    ImmutableBiMap<Method, QName> getRpcMethodToSchemaPath(final Class<? extends RpcService> key) {
        final Module module = getModule(key);
        final ImmutableBiMap.Builder<Method, QName> ret = ImmutableBiMap.builder();
        try {
            for (final RpcDefinition rpcDef : module.getRpcs()) {
                final Method method = findRpcMethod(key, rpcDef);
                ret.put(method, rpcDef.getQName());
            }
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException("Rpc defined in model does not have representation in generated class.", e);
        }
        return ret.build();
    }

    private Method findRpcMethod(final Class<? extends RpcService> key, final RpcDefinition rpcDef)
            throws NoSuchMethodException {
        final String methodName = BindingMapping.getRpcMethodName(rpcDef.getQName());
        final Class<?> inputClz = adapter.getRuntimeContext().getRpcInput(rpcDef.getQName());
        return key.getMethod(methodName, inputClz);
    }

    private Module getModule(final Class<?> modeledClass) {
        final QNameModule moduleName = BindingReflections.getQNameModule(modeledClass);
        final BindingRuntimeContext localRuntimeContext = adapter.getRuntimeContext();
        final Module module = localRuntimeContext.getEffectiveModelContext().findModule(moduleName).orElse(null);
        if (module != null) {
            return module;
        }
        throw new IllegalStateException(String.format("Schema for %s is not available; expected module name: %s; "
                + "full BindingRuntimeContext available in trace log", modeledClass, moduleName));
    }
}
