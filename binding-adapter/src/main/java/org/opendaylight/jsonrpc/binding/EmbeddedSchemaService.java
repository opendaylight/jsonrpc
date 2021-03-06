/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMSchemaServiceExtension;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextListener;

/**
 * Dummy implementation of {@link DOMSchemaService}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 18, 2018
 */
public final class EmbeddedSchemaService implements DOMSchemaService {
    private final ListenerRegistry<EffectiveModelContextListener> listeners = ListenerRegistry.create();
    private final EffectiveModelContext schemaContext;

    public EmbeddedSchemaService(final EffectiveModelContext schemaContext) {
        this.schemaContext = Objects.requireNonNull(schemaContext);
    }

    @Override
    public EffectiveModelContext getGlobalContext() {
        return schemaContext;
    }

    @Override
    public ListenerRegistration<EffectiveModelContextListener> registerSchemaContextListener(
            EffectiveModelContextListener listener) {
        listener.onModelContextUpdated(schemaContext);
        return listeners.register(listener);
    }

    @Override
    public @NonNull ClassToInstanceMap<DOMSchemaServiceExtension> getExtensions() {
        return ImmutableClassToInstanceMap.of();
    }
}
