/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.model.InbandModelsService;
import org.opendaylight.jsonrpc.model.SchemaContextProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpoints;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangStatementStreamSource;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;

/**
 * {@link SchemaContextProvider} used for peers that provide required YANG modules by themselves.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jan 13, 2019
 */
public final class InbandModelsSchemaContextProvider implements SchemaContextProvider {
    private final TransportFactory transportFactory;

    public static InbandModelsSchemaContextProvider create(TransportFactory transportFactory) {
        return new InbandModelsSchemaContextProvider(transportFactory);
    }

    private InbandModelsSchemaContextProvider(final TransportFactory transportFactory) {
        this.transportFactory = Objects.requireNonNull(transportFactory);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public EffectiveModelContext createSchemaContext(Peer peer) {
        Objects.requireNonNull(peer.getRpcEndpoints(), "RPC endpoint is mandatory for for inband models");
        RpcEndpoints enpodint = peer.nonnullRpcEndpoints().values()
                .stream()
                .filter(rpc -> rpc.getPath().equals("{}"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing RPC endpoint for root path"));
        Objects.requireNonNull(enpodint.getEndpointUri().getValue(), "RPC endpoint not set");
        try (InbandModelsService requester = transportFactory.endpointBuilder()
                .requester()
                .createProxy(InbandModelsService.class, enpodint.getEndpointUri().getValue())) {
            final CrossSourceStatementReactor.BuildAction reactor = RFC7950Reactors.defaultReactor().newBuild();

            requester.getModules().forEach(m -> {
                try {
                    reactor.addSource(YangStatementStreamSource.create(YangTextSchemaSource.delegateForByteSource(
                            m.getName() + ".yang", ByteSource.wrap(m.getContent().getBytes(StandardCharsets.UTF_8)))));
                } catch (YangSyntaxErrorException | IOException e) {
                    throw new IllegalStateException("Failed to add YANG source for " + m.getName(), e);
                }
            });
            return reactor.buildEffective();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URI is invalid", e);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build SchemaContext", e);
        }
    }
}
