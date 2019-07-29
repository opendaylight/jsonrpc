/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.JsonElement;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.JsonRpcTransactionFacade;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DOMTransactionChain} implementation for Netconf connector.
 */
public class TxChain implements DOMTransactionChain {

    private static final Logger LOG = LoggerFactory.getLogger(TxChain.class);

    private final DOMDataBroker dataBroker;
    private final TransactionChainListener listener;

    /**
     * Transaction created by this chain that hasn't been submitted or cancelled yet.
     */
    private DOMDataWriteTransaction currentTransaction = null;
    private volatile boolean closed = false;
    private volatile boolean successful = true;
    private final SchemaContext schemaContext;
    private final String deviceName;
    private final JsonConverter jsonConverter;
    private final HierarchicalEnumMap<JsonElement, DataType, String> pathMap;
    private final TransportFactory transportFactory;


    public TxChain(final DOMDataBroker dataBroker, final TransactionChainListener listener,
            @Nonnull TransportFactory transportFactory, @Nonnull String deviceName,
            @Nonnull HierarchicalEnumMap<JsonElement, DataType, String> pathMap, @Nonnull JsonConverter jsonConverter,
            @Nonnull SchemaContext schemaContext) {
        this.dataBroker = dataBroker;
        this.listener = listener;
        this.transportFactory = Preconditions.checkNotNull(transportFactory);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(deviceName), "Peer name is missing");
        this.deviceName = deviceName;
        this.pathMap = Preconditions.checkNotNull(pathMap);
        this.schemaContext = Preconditions.checkNotNull(schemaContext);
        this.jsonConverter = Preconditions.checkNotNull(jsonConverter);

    }

    @Override
    public synchronized DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        checkOperationPermitted();
        return dataBroker.newReadOnlyTransaction();
    }

    @Override
    public synchronized DOMDataWriteTransaction newWriteOnlyTransaction() {
        checkOperationPermitted();
        final DOMDataWriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        Preconditions.checkState(writeTransaction instanceof JsonRpcTransactionFacade);
        final DOMDataWriteTransaction pendingWriteTx = writeTransaction;
        currentTransaction = pendingWriteTx;
        return pendingWriteTx;
    }

    @Override
    public synchronized DOMDataReadWriteTransaction newReadWriteTransaction() {
        return dataBroker.newReadWriteTransaction();
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            notifyChainListenerSuccess();
        }
    }

    /**
     * Checks, if chain isn't closed and if there is no not submitted write transaction waiting.
     */
    private void checkOperationPermitted() {
        if (closed) {
            throw new TransactionChainClosedException("Transaction chain was closed");
        }
        Preconditions.checkState(currentTransaction == null, "Last write transaction has not finished yet");
    }

    private void notifyChainListenerSuccess() {
        if (closed && successful) {
            listener.onTransactionChainSuccessful(this);
        }
    }

}
