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

import java.util.Collections;
import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


@SuppressWarnings("deprecation")
public class JsonRPCDataBroker implements DOMDataBroker, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCDataBroker.class);
    private final SchemaContext schemaContext;
    private final JsonConverter jsonConverter;
    private final Peer peer;
    private final TransportFactory transportFactory;
    private final HierarchicalEnumMap<JsonElement, DataType, String> pathMap;
    private static final JsonObject TOP = new JsonObject();

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
     * @see DOMDataBroker
     */
    public JsonRPCDataBroker(Peer peer, SchemaContext schemaContext,
            HierarchicalEnumMap<JsonElement, DataType, String> pathMap,
            TransportFactory transportFactory, RemoteGovernance governance) {
        this.peer = Preconditions.checkNotNull(peer);
        this.schemaContext = Preconditions.checkNotNull(schemaContext);
        this.jsonConverter = new JsonConverter(schemaContext);
        this.transportFactory = Preconditions.checkNotNull(transportFactory);
        this.pathMap = Preconditions.checkNotNull(pathMap);

        if (peer.getDataConfigEndpoints() != null) {
            Util.populateFromEndpointList(pathMap, peer.getDataConfigEndpoints(), DataType.CONFIGURATION_DATA);
        } else {
            pathMap.put(TOP, DataType.CONFIGURATION_DATA,
                    governance.governance(store2str(store2int(LogicalDatastoreType.CONFIGURATION)), peer.getName(), TOP));
        }

        if (peer.getDataOperationalEndpoints() != null) {
            Util.populateFromEndpointList(pathMap, peer.getDataOperationalEndpoints(), DataType.OPERATIONAL_DATA);
        } else {
            pathMap.put(TOP, DataType.OPERATIONAL_DATA,
                    governance.governance(store2str(store2int(LogicalDatastoreType.OPERATIONAL)), peer.getName(), TOP));
        }
        LOG.info("Broker Instantiated for {}", peer.getName());
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return new JsonRPCTx(transportFactory, peer.getName(), pathMap, jsonConverter, schemaContext);
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return new JsonRPCTx(transportFactory, peer.getName(), pathMap, jsonConverter, schemaContext);
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return new JsonRPCTx(transportFactory, peer.getName(), pathMap, jsonConverter, schemaContext);
    }

    @Override
    public DOMTransactionChain createTransactionChain(TransactionChainListener listener) {
        throw new UnsupportedOperationException("Transaction chains not supported for json rpc mount point");
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(LogicalDatastoreType store, YangInstanceIdentifier path, DOMDataChangeListener listener, DataChangeScope triggeringScope) {
        throw new UnsupportedOperationException("Listener registrations are not supported by this DataBroker");
    }

    @Override
    public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
        return Collections.emptyMap();
    }
}
