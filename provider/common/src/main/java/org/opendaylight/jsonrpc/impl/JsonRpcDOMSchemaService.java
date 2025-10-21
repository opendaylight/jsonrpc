/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.annotations.Beta;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.util.ObjectRegistry;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.stmt.ModuleEffectiveStatement;
import org.opendaylight.yangtools.yang.model.spi.SimpleSchemaContext;

@Beta
public class JsonRpcDOMSchemaService implements DOMSchemaService, AutoCloseable {
    private static final EffectiveModelContext EMPTY = new EmptySchema();

    private final ObjectRegistry<Consumer<EffectiveModelContext>> listenerRegistry;
    private final @NonNull EffectiveModelContext context;

    private static final class EmptySchema extends SimpleSchemaContext implements EffectiveModelContext {
        protected EmptySchema() {
            super(Set.of());
        }

        @Override
        public Map<QNameModule, ModuleEffectiveStatement> getModuleStatements() {
            return Map.of();
        }
    }

    public JsonRpcDOMSchemaService(@NonNull Peer peer, @NonNull EffectiveModelContext context) {
        this.context = Objects.requireNonNull(context);
        listenerRegistry = ObjectRegistry.createConcurrent(peer.getName());
    }

    @Override
    public EffectiveModelContext getGlobalContext() {
        return context;
    }

    @Override
    public Registration registerSchemaContextListener(Consumer<EffectiveModelContext> listener) {
        return listenerRegistry.register(listener);
    }

    @Override
    public void close() {
        listenerRegistry.streamObjects().forEach(listener -> listener.accept(EMPTY));
        listenerRegistry.streamRegistrations().forEach(ObjectRegistration::close);
    }
}
