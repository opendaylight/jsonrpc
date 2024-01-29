/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.opendaylight.jsonrpc.provider.common.Util.store2int;
import static org.opendaylight.jsonrpc.provider.common.Util.store2str;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.AddListenerArgument;
import org.opendaylight.jsonrpc.model.DeleteListenerArgument;
import org.opendaylight.jsonrpc.model.JsonRpcTransactionFacade;
import org.opendaylight.jsonrpc.model.ListenerKey;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.jsonrpc.provider.common.Util;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonRPCDataBroker extends RemoteShardAware
        implements DOMDataBroker, DOMDataBroker.DataTreeChangeExtension {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCDataBroker.class);
    private static final JsonObject TOP = new JsonObject();

    /**
     * Instantiates a new JSON-RPC data broker.
     *
     * @param peer {@link Peer} to create Data broker for
     * @param schemaContext the schema context
     * @param pathMap shared instance of {@link HierarchicalEnumMap}
     * @param transportFactory {@link TransportFactory} used to create RPC connections
     * @param governance {@link RemoteGovernance} used to provide additional governance info
     * @param codecFactory shared {@link JsonRpcCodecFactory}
     * @see DOMDataBroker
     */
    public JsonRPCDataBroker(@NonNull Peer peer, @NonNull EffectiveModelContext schemaContext,
            @NonNull HierarchicalEnumMap<JsonElement, DataType, String> pathMap,
            @NonNull TransportFactory transportFactory, @Nullable RemoteGovernance governance,
            @NonNull JsonRpcCodecFactory codecFactory) {
        super(schemaContext, transportFactory, pathMap, codecFactory, peer);

        if (peer.getDataConfigEndpoints() != null) {
            Util.populateFromEndpointList(pathMap, peer.nonnullDataConfigEndpoints().values(),
                    DataType.CONFIGURATION_DATA);
        } else {
            if (governance != null) {
                pathMap.put(TOP, DataType.CONFIGURATION_DATA, governance
                        .governance(store2str(store2int(LogicalDatastoreType.CONFIGURATION)), peer.getName(), TOP));
            }
        }

        if (peer.getDataOperationalEndpoints() != null) {
            Util.populateFromEndpointList(pathMap, peer.nonnullDataOperationalEndpoints().values(),
                    DataType.OPERATIONAL_DATA);
        } else {
            if (governance != null) {
                pathMap.put(TOP, DataType.OPERATIONAL_DATA, governance
                        .governance(store2str(store2int(LogicalDatastoreType.OPERATIONAL)), peer.getName(), TOP));
            }
        }
        LOG.info("Broker Instantiated for {}", peer.getName());
    }

    @Override
    public JsonRpcTransactionFacade newReadOnlyTransaction() {
        return newReadWriteTransaction();
    }

    @Override
    public JsonRpcTransactionFacade newWriteOnlyTransaction() {
        return newReadWriteTransaction();
    }

    @Override
    public JsonRpcTransactionFacade newReadWriteTransaction() {
        return TransactionProxy.create(new JsonRPCTx(transportFactory, peer, pathMap, codecFactory, schemaContext));
    }

    @Override
    public DOMTransactionChain createTransactionChain() {
        return new TxChain(this, transportFactory, pathMap, codecFactory, schemaContext, peer);
    }

    @Override
    public DOMTransactionChain createMergingTransactionChain() {
        return createTransactionChain();
    }

    @Override
    public List<? extends Extension> supportedExtensions() {
        return List.of(this);
    }

    @Override
    public Registration registerDataTreeChangeListener(DOMDataTreeIdentifier treeId,
            DOMDataTreeChangeListener listener) {
        final JsonElement busPath = codecFactory.pathCodec().serialize(treeId.path());
        final RemoteOmShard shard = getShard(treeId.datastore(), busPath);
        final DOMDataTreeChangeListenerAdapter adapter;
        final ListenerKey listenerKey;
        try {
            listenerKey = shard.addListener(new AddListenerArgument(
                    String.valueOf(Util.store2int(treeId.datastore())), "", busPath, null));
            adapter = new DOMDataTreeChangeListenerAdapter(listener, transportFactory, listenerKey.getUri(),
                    codecFactory, schemaContext);
        } catch (URISyntaxException e) {
            // remote shard provided us wrong URI
            throw new IllegalStateException("Invalid URI provided from remote shard", e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create subscriber", e);
        }

        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                shard.deleteListener(new DeleteListenerArgument(listenerKey.getUri(), listenerKey.getName()));
                adapter.close();
            }
        };
    }
}
