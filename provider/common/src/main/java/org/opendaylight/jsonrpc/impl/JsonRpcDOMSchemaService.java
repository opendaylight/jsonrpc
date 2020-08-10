/*
 * Copyright (c) 2020 dNation.tech. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.opendaylight.mdsal.dom.spi.AbstractDOMSchemaService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextListener;
import org.opendaylight.yangtools.yang.model.api.stmt.ModuleEffectiveStatement;
import org.opendaylight.yangtools.yang.model.util.SimpleSchemaContext;

@Beta
public class JsonRpcDOMSchemaService extends AbstractDOMSchemaService implements AutoCloseable {
    private static final EffectiveModelContext EMPTY = new EmptySchema();
    private final ListenerRegistry<EffectiveModelContextListener> listenerRegistry;
    private final EffectiveModelContext context;

    private static final class EmptySchema extends SimpleSchemaContext implements EffectiveModelContext {
        protected EmptySchema() {
            super(Collections.emptySet());
        }

        @Override
        public Map<QNameModule, ModuleEffectiveStatement> getModuleStatements() {
            return Collections.emptyMap();
        }
    }

    public JsonRpcDOMSchemaService(@NonNull Peer peer, @NonNull EffectiveModelContext context) {
        this.context = Objects.requireNonNull(context);
        listenerRegistry = ListenerRegistry.create(peer.getName());
    }

    @Override
    public EffectiveModelContext getGlobalContext() {
        return context;
    }

    @Override
    public ListenerRegistration<EffectiveModelContextListener> registerSchemaContextListener(
            EffectiveModelContextListener listener) {
        return listenerRegistry.register(listener);
    }

    @Override
    public void close() {
        listenerRegistry.streamListeners().forEach(listener -> listener.onModelContextUpdated(EMPTY));
        listenerRegistry.clear();
    }
}
