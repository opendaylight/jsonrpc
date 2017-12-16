/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.jsonrpc.model.SchemaContextProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.util.SimpleSchemaContext;

/**
 * {@link SchemaContextProvider} which uses global {@link SchemaContext}. This
 * implementation only validates that required models are present in global
 * {@link SchemaContext}.Implementation is fail-fast, so any missing model will
 * cause error.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class BuiltinSchemaContextProvider implements SchemaContextProvider {
    private final SchemaContext schemaContext;

    public BuiltinSchemaContextProvider(@Nonnull final SchemaContext schemaContext) {
        this.schemaContext = Preconditions.checkNotNull(schemaContext);
    }

    @Override
    public SchemaContext createSchemaContext(Peer peer) {
        Set<Module> moduleIds = peer.getModules().stream().map(YangIdentifier::getValue).map(m -> {
            final Optional<Module> possibleModule = Util.findModuleWithLatestRevision(schemaContext, m);
            Preconditions.checkState(possibleModule.isPresent(), "No model '%s' in global schema context", m);
            return possibleModule.get();
        }).collect(Collectors.toSet());
        return buildSchemaContext(moduleIds);
    }

    private SchemaContext buildSchemaContext(Set<Module> modules) {
        final Set<Module> resolved = Sets.newHashSet();
        resolved.addAll(modules);
        modules.stream()
                .forEach(m -> resolved.addAll(m.getImports().stream()
                        .map(mi -> schemaContext.findModule(mi.getModuleName(), mi.getRevision()).get())
                        .collect(Collectors.toSet())));
        return SimpleSchemaContext.forModules(resolved);
    }
}
