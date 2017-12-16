/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import javax.annotation.Nonnull;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.SchemaContextProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangStatementStreamSource;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
//import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public GovernanceSchemaContextProvider(@Nonnull final RemoteGovernance governance) {
        this.governance = Preconditions.checkNotNull(governance);
    }

    @Override
    public SchemaContext createSchemaContext(Peer peer) {
        final CrossSourceStatementReactor.BuildAction reactor = RFC7950Reactors.defaultReactor().newBuild();
        try {
            peer.getModules().stream().map(YangIdentifier::getValue).forEach(m -> {
                LOG.trace("Fetching yang model '{}'", m);
                final String model = governance.source(m);
                Preconditions.checkState(!Strings.isNullOrEmpty(model), "Module not found in remote governance : %s",
                        m);
                try {
                    reactor.addSource(
                            YangStatementStreamSource.create(
                                YangTextSchemaSource.delegateForByteSource(
                                    m + ".yang", ByteSource.wrap(model.getBytes(Charsets.UTF_8)))
                                )
                            );
                } catch (java.io.IOException e) {
                    /* This should never occur - our byte stream is off a pre-loaded
                     * byte array
                     **/
                    throw new IllegalStateException("Cannot obtain models", e);
                } catch (YangSyntaxErrorException e) {
                    throw new IllegalStateException("Cannot parse models", e);
                }
            });
            return reactor.buildEffective();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot obtain models", e);
        }
    }
}
