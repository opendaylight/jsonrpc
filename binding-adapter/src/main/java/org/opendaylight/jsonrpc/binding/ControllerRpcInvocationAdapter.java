/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import java.util.Objects;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

/**
 * Implementation of {@link RpcInvocationAdapter} used within ODL controller.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 23, 2018
 */
public class ControllerRpcInvocationAdapter implements RpcInvocationAdapter {
    private SchemaChangeAwareConverter converter;
    private final BindingNormalizedNodeSerializer codec;
    private final DOMSchemaService schemaService;

    public ControllerRpcInvocationAdapter(DOMSchemaService schemaService, BindingNormalizedNodeSerializer codec) {
        this.schemaService = Objects.requireNonNull(schemaService);
        converter = new SchemaChangeAwareConverter(schemaService);
        this.codec = codec;
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
    public Registration registerImpl(Rpc<?, ?> impl) {
        return () -> {
            // NOOP
        };
    }

    @Override
    public EffectiveModelContext schemaContext() {
        return schemaService.getGlobalContext();
    }

    @Override
    public BindingRuntimeContext getRuntimeContext() {
        return null;
    }
}
