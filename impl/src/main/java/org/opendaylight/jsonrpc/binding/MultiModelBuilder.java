/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.RpcService;

/**
 * Builder for convenient way to define multiple RpcService implementations.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Oct 16, 2018
 */
public final class MultiModelBuilder implements Builder<ClassToInstanceMap<RpcService>> {
    private final ClassToInstanceMap<RpcService> services = MutableClassToInstanceMap.create();

    private MultiModelBuilder() {
        // no direct instantiation
    }

    public static MultiModelBuilder create() {
        return new MultiModelBuilder();
    }

    public <T extends RpcService> MultiModelBuilder addService(Class<T> type, T impl) {
        services.put(type, impl);
        return this;
    }

    @Override
    public ClassToInstanceMap<RpcService> build() {
        Preconditions.checkState(!services.isEmpty(), "No services defined");
        return ImmutableClassToInstanceMap.copyOf(services);
    }
}
