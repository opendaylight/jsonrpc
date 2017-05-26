/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import java.io.ByteArrayInputStream;

import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.SchemaContextProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangStatementSourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Implementation of {@link SchemaContextProvider} which uses
 * {@link RemoteGovernance} to obtain models from.Implementation is fail-fast,
 * so any missing model will cause error.
 * 
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class GovernanceSchemaContextProvider implements SchemaContextProvider {
    private static final Logger LOG = LoggerFactory.getLogger(GovernanceSchemaContextProvider.class);
    private final RemoteGovernance governance;

    public GovernanceSchemaContextProvider(final RemoteGovernance governance) {
        this.governance = Preconditions.checkNotNull(governance);
    }

    @Override
    public SchemaContext createSchemaContext(Peer peer) {
        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        try {
            peer.getModules().stream().map(YangIdentifier::getValue).forEach(m -> {
                LOG.trace("Fetching yang model '{}'", m);
                final String model = governance.source(m);
                Preconditions.checkState(!Strings.isNullOrEmpty(model), "Module not found in remote governance : %s",
                        m);
                reactor.addSource(
                        new YangStatementSourceImpl(new ByteArrayInputStream(model.getBytes(Charsets.UTF_8))));
            });
            return reactor.buildEffective();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot obtain models", e);
        }
    }
}
