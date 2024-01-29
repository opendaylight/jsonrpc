/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public interface RpcInvocationAdapter {
    SchemaChangeAwareConverter converter();

    BindingNormalizedNodeSerializer codec();

    <T extends Rpc<?, ?>> ObjectRegistration<T> registerImpl(T impl);

    EffectiveModelContext schemaContext();

    BindingRuntimeContext getRuntimeContext();
}
