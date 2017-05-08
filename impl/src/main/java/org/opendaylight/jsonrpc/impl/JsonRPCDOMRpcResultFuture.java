/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others. All rights reserved.
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.jsonrpc.model.RpcExceptionImpl;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Simplistic implementation of CheckedFuture to be returned by the 
 * RPC implementation
 * 
 */

final class JsonRPCDOMRpcResultFuture implements CheckedFuture<DOMRpcResult, DOMRpcException> { 

    private volatile Exception exception = null;

    private SettableFuture<DOMRpcResult> jsonRPCFuture;

    private JsonRPCDOMRpcResultFuture(SettableFuture<DOMRpcResult> jsonRPCFuture) {
        this.jsonRPCFuture = jsonRPCFuture;
    }

    static CheckedFuture<DOMRpcResult, DOMRpcException> create(SettableFuture<DOMRpcResult> jsonRPCFuture) {
        return new JsonRPCDOMRpcResultFuture(jsonRPCFuture);
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return jsonRPCFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public void addListener(final Runnable listener, final Executor executor) {
        jsonRPCFuture.addListener(listener, executor);
    }

    public boolean set(DOMRpcResult value) {
        return jsonRPCFuture.set(value);
    }

    public boolean setException(Exception e) {
        this.exception = e;
        jsonRPCFuture.set(null);
        return true; 
    }

    @Override
    public DOMRpcResult get() throws InterruptedException, ExecutionException {
        DOMRpcResult result =  jsonRPCFuture.get();
        if (this.exception == null) {
            return result;
        } else {
            throw new ExecutionException(this.exception);
        }
    }
    @Override
    public DOMRpcResult get(final long timeout, final TimeUnit unit) throws InterruptedException,
            TimeoutException, ExecutionException {
        DOMRpcResult result =  jsonRPCFuture.get(timeout, unit);
        if (this.exception == null) {
            return result;
        } else {
            throw new ExecutionException(this.exception);
        }
    }

    @Override
    public DOMRpcResult checkedGet() throws DOMRpcException {
        try {
            return get();
        } catch (InterruptedException | ExecutionException e) {
            if (this.exception != null) {
                Throwables.propagate(exception);
                return null;
            } else {
                throw new RpcExceptionImpl(e.getMessage());
            }
        }
    }

    @Override
    public DOMRpcResult checkedGet(final long timeout, final TimeUnit unit) throws TimeoutException, DOMRpcException {
        try {
            return get(timeout, unit);
        } catch (InterruptedException | ExecutionException e) {
            if (this.exception != null) {
                Throwables.propagate(exception);
                return null;
            } else {
                throw new RpcExceptionImpl(e.getMessage());
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return jsonRPCFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return jsonRPCFuture.isDone();
    }
}
