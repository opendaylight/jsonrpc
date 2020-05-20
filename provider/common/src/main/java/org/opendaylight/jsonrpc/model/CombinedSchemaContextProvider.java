/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.provider.common.BuiltinSchemaContextProvider;
import org.opendaylight.jsonrpc.provider.common.GovernanceSchemaContextProvider;
import org.opendaylight.jsonrpc.provider.common.InbandModelsSchemaContextProvider;
import org.opendaylight.jsonrpc.provider.common.ProviderDependencies;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public class CombinedSchemaContextProvider implements SchemaContextProvider {
    private final GovernanceProvider governanceProvider;
    private final ProviderDependencies dependencies;

    public CombinedSchemaContextProvider(@NonNull GovernanceProvider governanceProvider,
            @NonNull ProviderDependencies dependencies) {
        this.dependencies = Objects.requireNonNull(dependencies);
        this.governanceProvider = Objects.requireNonNull(governanceProvider);
    }

    private SchemaContextProvider getProvider(Peer peer) {
        if (Util.supportInbandModels(peer)) {
            return InbandModelsSchemaContextProvider.create(dependencies.getTransportFactory());
        }
        if (governanceProvider.get().isPresent()) {
            return new GovernanceSchemaContextProvider(governanceProvider.get().get(),
                    dependencies.getYangXPathParserFactory());
        }
        return new BuiltinSchemaContextProvider(dependencies.getSchemaService().getGlobalContext());
    }

    @Override
    public EffectiveModelContext createSchemaContext(@NonNull Peer peer) {
        Objects.requireNonNull(peer);
        return getProvider(peer).createSchemaContext(peer);
    }
}
