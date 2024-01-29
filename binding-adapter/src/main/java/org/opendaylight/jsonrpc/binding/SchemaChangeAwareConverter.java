/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * {@link Supplier} of {@link JsonRpcCodecFactory} that is up-to-date with changes in global {@link SchemaContext}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Aug 25, 2018
 */
public final class SchemaChangeAwareConverter
        implements Supplier<JsonRpcCodecFactory>, AutoCloseable {
    private final AtomicReference<JsonRpcCodecFactory> converter = new AtomicReference<>(null);
    private Registration registration;

    public SchemaChangeAwareConverter(@NonNull DOMSchemaService domSchemaService) {
        Objects.requireNonNull(domSchemaService);
        this.registration = domSchemaService.registerSchemaContextListener(this::onModelContextUpdated);
        refresh(domSchemaService.getGlobalContext());
    }

    private void refresh(EffectiveModelContext schemaContext) {
        converter.set(new JsonRpcCodecFactory(schemaContext));
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

    @Override
    public JsonRpcCodecFactory get() {
        return converter.get();
    }

    public void onModelContextUpdated(EffectiveModelContext newModelContext) {
        refresh(newModelContext);
    }
}
