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
import org.opendaylight.yangtools.binding.Rpc;

/**
 * Builder for convenient way to define multiple RpcService implementations.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Oct 16, 2018
 */
public final class MultiModelBuilder {
    private final ClassToInstanceMap<Rpc<?, ?>> services = MutableClassToInstanceMap.create();

    private MultiModelBuilder() {
        // no direct instantiation
    }

    public static MultiModelBuilder create() {
        return new MultiModelBuilder();
    }

    public MultiModelBuilder addService(Rpc<?, ?> impl) {
        services.put(impl.implementedInterface(), impl);
        return this;
    }

    public ClassToInstanceMap<Rpc<?, ?>> build() {
        Preconditions.checkState(!services.isEmpty(), "No services defined");
        return ImmutableClassToInstanceMap.copyOf(services);
    }
}
