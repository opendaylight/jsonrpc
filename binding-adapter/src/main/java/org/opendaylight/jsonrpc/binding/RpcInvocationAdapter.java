/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import org.opendaylight.yangtools.binding.Rpc;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public interface RpcInvocationAdapter {
    SchemaChangeAwareConverter converter();

    BindingNormalizedNodeSerializer codec();

    Registration registerImpl(Rpc<?, ?> impl);

    EffectiveModelContext schemaContext();

    BindingRuntimeContext getRuntimeContext();
}
