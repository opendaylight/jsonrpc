/*
 * Copyright (c) 2018 Lumina Networks, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.jsonrpc.impl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonElement;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.mdsal.common.api.TransactionChainClosedException;
import org.opendaylight.mdsal.common.api.TransactionChainListener;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class TxChainTest {
    @Mock
    private SchemaContext schemaContext;
    @Mock
    private JsonConverter jsonConverter;
    @Mock
    private TransportFactory transportFactory;
    @Mock
    private HierarchicalEnumMap<JsonElement, DataType, String> pathMap;
    @Mock
    private DOMDataBroker broker;
    @Mock
    private TransactionChainListener listener;
    @Mock
    private DOMDataTreeReadTransaction readOnlyTx;
    private TxChain chain;
    private JsonRPCTx writeOnlyTx1;
    private JsonRPCTx writeOnlyTx2;
    private JsonRPCTx writeOnlyTx3;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        writeOnlyTx1 = new JsonRPCTx(transportFactory, "test", pathMap, jsonConverter, schemaContext);
        writeOnlyTx2 = new JsonRPCTx(transportFactory, "test", pathMap, jsonConverter, schemaContext);
        writeOnlyTx3 = new JsonRPCTx(transportFactory, "test", pathMap, jsonConverter, schemaContext);

        when(broker.newReadOnlyTransaction()).thenReturn(readOnlyTx);
        when(broker.newWriteOnlyTransaction()).thenReturn(writeOnlyTx1)
                .thenReturn(writeOnlyTx2)
                .thenReturn(writeOnlyTx3);
        when(broker.newReadWriteTransaction()).thenReturn(writeOnlyTx1)
                .thenReturn(writeOnlyTx2)
                .thenReturn(writeOnlyTx3);
        chain = new TxChain(broker, listener, transportFactory, pathMap, jsonConverter, schemaContext);
    }

    @Test()
    public void testNewReadOnlyTransactionPrevSubmitted() throws Exception {
        chain.newWriteOnlyTransaction();
        chain.onSubmit(writeOnlyTx1);
        chain.newReadOnlyTransaction();
    }

    @Test(expected = IllegalStateException.class)
    public void testNewReadOnlyTransactionPrevNotSubmitted() throws Exception {
        chain.newWriteOnlyTransaction();
        chain.newReadOnlyTransaction();
    }

    @Test
    public void testNewReadWriteTransactionPrevSubmitted() throws Exception {
        chain.newReadWriteTransaction();
        chain.onSubmit(writeOnlyTx1);
        chain.newReadWriteTransaction();
    }

    @Test(expected = IllegalStateException.class)
    public void testNewReadWriteTransactionPrevNotSubmitted() throws Exception {
        chain.newReadWriteTransaction();
        chain.newReadWriteTransaction();
    }

    @Test(expected = IllegalStateException.class)
    public void testNewWriteOnlyTransactionPrevNotSubmitted() throws Exception {
        chain.newWriteOnlyTransaction();
        chain.newWriteOnlyTransaction();
    }

    @Test(expected = TransactionChainClosedException.class)
    public void testCloseAfterFinished() throws Exception {
        chain.close();
        verify(listener).onTransactionChainSuccessful(chain);
        chain.newReadOnlyTransaction();
    }

    @Test
    public void testChainFail() throws Exception {
        final JsonRPCTx writeTx = chain.newWriteOnlyTransaction();
        writeTx.commit().get();
        final TransactionCommitFailedException cause = new TransactionCommitFailedException("fail");
        chain.onFailure(writeOnlyTx1, cause);
        verify(listener).onTransactionChainFailed(chain, writeOnlyTx1, cause);
    }

    @Test
    public void testChainSuccess() throws Exception {
        final JsonRPCTx writeTx = chain.newWriteOnlyTransaction();
        chain.close();
        writeTx.commit().get();
        verify(listener).onTransactionChainSuccessful(chain);
    }

    @Test
    public void testCancel() throws Exception {
        final JsonRPCTx writeTx = chain.newWriteOnlyTransaction();
        writeTx.cancel();
        chain.newWriteOnlyTransaction();
    }

    @Test
    public void testMultiplePendingTransactions() throws Exception {
        // create 1st tx
        final JsonRPCTx writeTx1 = chain.newWriteOnlyTransaction();
        // submit 1st tx
        writeTx1.commit().get();
        chain.onSubmit(writeOnlyTx1);

        // create 2nd tx
        final JsonRPCTx writeTx2 = chain.newWriteOnlyTransaction();
        // submit 2nd tx
        writeTx2.commit().get();
        chain.onSubmit(writeOnlyTx2);

        // create 3rd tx
        final JsonRPCTx writeTx3 = chain.newWriteOnlyTransaction();
        // cancel 3rd tx
        writeTx3.cancel();
        chain.onCancel(writeOnlyTx3);

        // complete first two transactions successfully
        chain.onSuccess(writeOnlyTx1);
        chain.onSuccess(writeOnlyTx2);

        // close chain
        chain.close();

        verify(listener).onTransactionChainSuccessful(chain);
    }

    @Test
    public void testMultiplePendingTransactionsFail() throws Exception {
        // create 1st tx
        final JsonRPCTx writeTx1 = chain.newWriteOnlyTransaction();
        // submit 1st tx
        writeTx1.commit().get();
        chain.onSubmit(writeOnlyTx1);

        // create 2nd tx
        final JsonRPCTx writeTx2 = chain.newWriteOnlyTransaction();
        // submit 2nd tx
        writeTx2.commit().get();
        chain.onSubmit(writeOnlyTx2);

        // create 3rd tx
        final JsonRPCTx writeTx3 = chain.newWriteOnlyTransaction();

        chain.close();

        // fail 1st transaction
        final Exception cause1 = new Exception("fail");
        chain.onFailure(writeOnlyTx1, cause1);
        chain.onCancel(writeTx3);
        // 2nd transaction success
        chain.onSuccess(writeOnlyTx2);

        verify(listener).onTransactionChainFailed(chain, writeOnlyTx1, cause1);
        // 1 transaction failed, onTransactionChainSuccessful must not be called
        verify(listener, never()).onTransactionChainSuccessful(chain);
    }
}
