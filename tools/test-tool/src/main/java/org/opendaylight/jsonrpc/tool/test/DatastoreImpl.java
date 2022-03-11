/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.tool.test;

import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.ResponderSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.impl.JsonRpcDatastoreAdapter;
import org.opendaylight.jsonrpc.model.MutablePeer;
import org.opendaylight.jsonrpc.provider.common.GovernanceSchemaContextProvider;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.FixedDOMSchemaService;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DatastoreImpl extends JsonRpcDatastoreAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreImpl.class);
    private final ResponderSession session;

    DatastoreImpl(JsonRpcCodecFactory codecFactory, DOMDataBroker domDataBroker, EffectiveModelContext schemaContext,
            TransportFactory transportFactory, String endpoint) throws URISyntaxException {
        super(codecFactory, domDataBroker, schemaContext, transportFactory);
        session = transportFactory.endpointBuilder().responder().create(endpoint, this);
    }

    static DatastoreImpl create(TransportFactory transportFactory, String endpoint, Set<YangIdentifier> modules,
            GovernanceSchemaContextProvider schemaProvider) throws URISyntaxException {
        final EffectiveModelContext schemaContext = schemaProvider
                .createSchemaContext(new MutablePeer().addModels(new ArrayList<>(modules)));
        LOG.info("Schema : {}", schemaContext);
        final DOMSchemaService domSchemaService = FixedDOMSchemaService
                .of(new FixedEffectiveModelContextProvider(schemaContext));
        final JsonRpcCodecFactory codecFactory = new JsonRpcCodecFactory(schemaContext);
        final DOMDataBroker domDataBroker = createDomDataBroker(domSchemaService,
                MoreExecutors.listeningDecorator(Executors.newCachedThreadPool()));
        return new DatastoreImpl(codecFactory, domDataBroker, schemaContext, transportFactory, endpoint);
    }

    private static class FixedEffectiveModelContextProvider implements EffectiveModelContextProvider {
        private final EffectiveModelContext context;

        FixedEffectiveModelContextProvider(EffectiveModelContext context) {
            this.context = context;
        }

        @Override
        public @NonNull EffectiveModelContext getEffectiveModelContext() {
            return context;
        }
    }

    private static DOMDataBroker createDomDataBroker(DOMSchemaService schemaService,
            ListeningExecutorService executor) {
        final Map<LogicalDatastoreType, ? extends DOMStore> stores = ImmutableMap
                .<LogicalDatastoreType, InMemoryDOMDataStore>builder()
                .put(OPERATIONAL, InMemoryDOMDataStoreFactory.create(OPERATIONAL.name(), schemaService))
                .put(CONFIGURATION, InMemoryDOMDataStoreFactory.create(CONFIGURATION.name(), schemaService))
                .build();

        return new SerializedDOMDataBroker(ImmutableMap.copyOf(stores), executor);
    }

    @Override
    public void close() {
        session.close();
        super.close();
    }

    @Override
    public String toString() {
        return "DatastoreImpl [session=" + session + "]";
    }
}
