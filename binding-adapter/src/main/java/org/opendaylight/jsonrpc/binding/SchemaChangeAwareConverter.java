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
import org.opendaylight.jsonrpc.impl.JsonConverter;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * {@link Supplier} of {@link JsonConverter} that is up-to-date with changes in global {@link SchemaContext}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Aug 25, 2018
 */
public class SchemaChangeAwareConverter
        implements Supplier<JsonConverter>, AutoCloseable, EffectiveModelContextListener {
    private final AtomicReference<JsonConverter> converter = new AtomicReference<>(null);
    private ListenerRegistration<EffectiveModelContextListener> registration;

    public SchemaChangeAwareConverter(@NonNull DOMSchemaService domSchemaService) {
        Objects.requireNonNull(domSchemaService);
        this.registration = domSchemaService.registerSchemaContextListener(this);
        refresh(domSchemaService.getGlobalContext());
    }

    private void refresh(EffectiveModelContext schemaContext) {
        converter.set(new JsonConverter(schemaContext));
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }

    @Override
    public JsonConverter get() {
        return converter.get();
    }

    @Override
    public void onModelContextUpdated(EffectiveModelContext newModelContext) {
        refresh(newModelContext);
    }
}
