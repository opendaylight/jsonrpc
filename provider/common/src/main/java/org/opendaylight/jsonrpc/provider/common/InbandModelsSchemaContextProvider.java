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
import org.opendaylight.yangtools.yang.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.parser.api.YangSyntaxErrorException;

/**
 * {@link SchemaContextProvider} used for peers that provide required YANG modules by themselves.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jan 13, 2019
 */
public final class InbandModelsSchemaContextProvider implements SchemaContextProvider {
    private final TransportFactory transportFactory;
    private final YangParserFactory yangParserFactory;

    public static InbandModelsSchemaContextProvider create(TransportFactory transportFactory,
            YangParserFactory yangParserFactory) {
        return new InbandModelsSchemaContextProvider(transportFactory, yangParserFactory);
    }

    private InbandModelsSchemaContextProvider(final TransportFactory transportFactory,
            YangParserFactory yangParserFactory) {
        this.transportFactory = Objects.requireNonNull(transportFactory);
        this.yangParserFactory = Objects.requireNonNull(yangParserFactory);
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
            final var parser = yangParserFactory.createParser();
            requester.getModules().forEach(m -> {
                try {
                    parser.addSource(new DelegatedYangTextSource(
                        new SourceIdentifier(m.getName()), CharSource.wrap(m.getContent())));
                } catch (YangSyntaxErrorException | IOException e) {
                    throw new IllegalStateException("Failed to add YANG source for " + m.getName(), e);
                }
            });
            return parser.buildEffectiveModel();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URI is invalid", e);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to build SchemaContext", e);
        }
    }
}
