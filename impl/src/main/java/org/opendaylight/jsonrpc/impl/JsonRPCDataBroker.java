/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.opendaylight.jsonrpc.impl.Util.store2int;
import static org.opendaylight.jsonrpc.impl.Util.store2str;

import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.ListenerKey;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionChainListener;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRPCDataBroker extends RemoteShardAware implements DOMDataBroker, DOMDataTreeChangeService {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCDataBroker.class);
    private static final JsonObject TOP = new JsonObject();
    private final ClassToInstanceMap<DOMDataBrokerExtension> extensions;
    private final Peer peer;

    /**
     * Instantiates a new JSON-RPC data broker.
     *
     * @param peer {@link Peer} to create Data broker for
     * @param schemaContext the schema context
     * @param pathMap shared instance of {@link HierarchicalEnumMap}
     * @param transportFactory {@link TransportFactory} used to create RPC
     *            connections
     * @param governance {@link RemoteGovernance} used to provide additional
     *            governance info
     * @param jsonConverter shared {@link JsonConverter}
     * @see DOMDataBroker
     */
    public JsonRPCDataBroker(@Nonnull Peer peer, @Nonnull SchemaContext schemaContext,
            @Nonnull HierarchicalEnumMap<JsonElement, DataType, String> pathMap,
            @Nonnull TransportFactory transportFactory, @Nullable RemoteGovernance governance,
            @Nonnull JsonConverter jsonConverter) {
        super(schemaContext, transportFactory, pathMap, jsonConverter);
        extensions = ImmutableClassToInstanceMap.<DOMDataBrokerExtension>builder()
                .put(DOMDataTreeChangeService.class, this)
                .build();
        this.peer = Preconditions.checkNotNull(peer);
        if (peer.getDataConfigEndpoints() != null) {
            Util.populateFromEndpointList(pathMap, peer.getDataConfigEndpoints(), DataType.CONFIGURATION_DATA);
        } else {
            if (governance != null) {
                pathMap.put(TOP, DataType.CONFIGURATION_DATA, governance
                        .governance(store2str(store2int(LogicalDatastoreType.CONFIGURATION)), peer.getName(), TOP));
            }
        }

        if (peer.getDataOperationalEndpoints() != null) {
            Util.populateFromEndpointList(pathMap, peer.getDataOperationalEndpoints(), DataType.OPERATIONAL_DATA);
        } else {
            if (governance != null) {
                pathMap.put(TOP, DataType.OPERATIONAL_DATA, governance
                        .governance(store2str(store2int(LogicalDatastoreType.OPERATIONAL)), peer.getName(), TOP));
            }
        }
        LOG.info("Broker Instantiated for {}", peer.getName());
    }

    @Override
    public DOMDataTreeReadTransaction newReadOnlyTransaction() {
        return new JsonRPCTx(transportFactory, peer.getName(), pathMap, jsonConverter, schemaContext);
    }

    @Override
    public DOMDataTreeWriteTransaction newWriteOnlyTransaction() {
        return new JsonRPCTx(transportFactory, peer.getName(), pathMap, jsonConverter, schemaContext);
    }

    @Override
    public DOMDataTreeReadWriteTransaction newReadWriteTransaction() {
        return new JsonRPCTx(transportFactory, peer.getName(), pathMap, jsonConverter, schemaContext);
    }

    @Override
    public DOMTransactionChain createTransactionChain(TransactionChainListener listener) {
        return new TxChain(this, listener, transportFactory, pathMap, jsonConverter, schemaContext);
    }

    @Override
    public @NonNull ClassToInstanceMap<DOMDataBrokerExtension> getExtensions() {
        return extensions;
    }

    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerDataTreeChangeListener(
            DOMDataTreeIdentifier treeId, L listener) {
        final JsonElement busPath = jsonConverter.toBus(treeId.getRootIdentifier(), null).getPath();
        final RemoteOmShard shard = getShard(treeId.getDatastoreType(), busPath);
        final DOMDataTreeChangeListenerAdapter adapter;
        final ListenerKey listenerKey;
        try {
            listenerKey = shard.addListener(Util.store2int(treeId.getDatastoreType()), "", busPath);
            adapter = new DOMDataTreeChangeListenerAdapter(listener, transportFactory, listenerKey.getUri(),
                    jsonConverter, schemaContext);
        } catch (URISyntaxException e) {
            // remote shard provided us wrong URI
            throw new IllegalStateException("Invalid URI provided from remote shard", e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create subscriber", e);
        }

        return new AbstractListenerRegistration<L>(listener) {
            @Override
            protected void removeRegistration() {
                shard.deleteListener(listenerKey.getUri(), listenerKey.getName());
                adapter.close();
            }
        };
    }
}
