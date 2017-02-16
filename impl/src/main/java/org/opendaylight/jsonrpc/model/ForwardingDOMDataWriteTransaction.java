/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * {@link DOMDataWriteTransaction} which just delegates method calls to other
 * {@link DOMDataWriteTransaction}.
 * 
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
@SuppressWarnings("deprecation")
public class ForwardingDOMDataWriteTransaction implements DOMDataWriteTransaction {
    protected DOMDataWriteTransaction delegate;

    public ForwardingDOMDataWriteTransaction(@Nonnull DOMDataWriteTransaction delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public boolean cancel() {
        return delegate.cancel();
    }

    @Override
    public void delete(LogicalDatastoreType store, YangInstanceIdentifier path) {
        delegate.delete(store, path);
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        return delegate.submit();
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return delegate.commit();
    }

    @Override
    public Object getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public void put(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        delegate.put(store, path, data);
    }

    @Override
    public void merge(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        delegate.merge(store, path, data);
    }
}
