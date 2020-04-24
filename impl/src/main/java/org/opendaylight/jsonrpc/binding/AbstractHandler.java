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
import org.opendaylight.yangtools.yang.binding.RpcService;
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
        rpcMethodMap = adapter.codec().getRpcMethodToSchemaPath(type)
                .entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), adapter.schemaContext()
                        .getOperations()
                        .stream()
                        .filter(r -> r.getPath().equals(e.getValue()))
                        .findFirst()
                        .get()))
                .collect(Collector.of(ImmutableBiMap::<Method, RpcDefinition>builder,
                    (builder, entry) -> builder.put(entry.getKey(), entry.getValue()),
                    (k, v) -> k.putAll(v.build()), ImmutableBiMap.Builder<Method, RpcDefinition>::build));
    }
}
