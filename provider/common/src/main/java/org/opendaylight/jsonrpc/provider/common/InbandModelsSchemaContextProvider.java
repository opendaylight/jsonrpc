/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import com.google.common.io.CharSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.model.InbandModelsService;
import org.opendaylight.jsonrpc.model.SchemaContextProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpoints;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.source.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.spi.source.DelegatedYangTextSource;
import org.opendaylight.yangtools.yang.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangStatementStreamSource;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.xpath.api.YangXPathParserFactory;

/**
 * {@link SchemaContextProvider} used for peers that provide required YANG modules by themselves.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jan 13, 2019
 */
public final class InbandModelsSchemaContextProvider implements SchemaContextProvider {
    private final TransportFactory transportFactory;
    private final YangXPathParserFactory xpathParserFactory;

    public static InbandModelsSchemaContextProvider create(TransportFactory transportFactory,
            YangXPathParserFactory xpathParserFactory) {
        return new InbandModelsSchemaContextProvider(transportFactory, xpathParserFactory);
    }

    private InbandModelsSchemaContextProvider(final TransportFactory transportFactory,
            YangXPathParserFactory xpathParserFactory) {
        this.transportFactory = Objects.requireNonNull(transportFactory);
        this.xpathParserFactory = Objects.requireNonNull(xpathParserFactory);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public EffectiveModelContext createSchemaContext(Peer peer) {
        Objects.requireNonNull(peer.getRpcEndpoints(), "RPC endpoint is mandatory for for inband models");
        RpcEndpoints enpodint = peer.nonnullRpcEndpoints()
                .values()
                .stream()
                .filter(rpc -> rpc.getPath().equals("{}"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing RPC endpoint for root path"));
        Objects.requireNonNull(enpodint.getEndpointUri().getValue(), "RPC endpoint not set");
        try (InbandModelsService requester = transportFactory.endpointBuilder()
                .requester()
                .createProxy(InbandModelsService.class, enpodint.getEndpointUri().getValue())) {
            final CrossSourceStatementReactor.BuildAction reactor = RFC7950Reactors
                    .defaultReactorBuilder(xpathParserFactory)
                    .build()
                    .newBuild();

            requester.getModules().forEach(m -> {
                try {
                    reactor.addSource(YangStatementStreamSource.create(new DelegatedYangTextSource(
                        new SourceIdentifier(m.getName()), CharSource.wrap(m.getContent()))));
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
