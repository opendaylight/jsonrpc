/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import com.google.common.reflect.AbstractInvocationHandler;
import java.util.Objects;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

/**
 * Abstract class that hold common fields for in/out handlers.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 20, 2018
 */
abstract class AbstractHandler<T extends Rpc<?, ?>> extends AbstractInvocationHandler {
    final RpcInvocationAdapter adapter;
    final RpcDefinition rpcDef;

    AbstractHandler(final Class<T> type, RpcInvocationAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter);
        this.rpcDef = (RpcDefinition) adapter.getRuntimeContext().getRpcDefinition(type).statement();
    }
}
