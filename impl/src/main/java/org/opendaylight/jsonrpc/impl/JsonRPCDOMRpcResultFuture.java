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
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.jsonrpc.model.RpcExceptionImpl;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;



/**
 * Simplistic implementation of CheckedFuture to be returned by the 
 * RPC implementation
 * 
 */

final class JsonRPCDOMRpcResultFuture implements CheckedFuture<DOMRpcResult, DOMRpcException> { 

    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCDOMRpcResultFuture.class);
    private volatile Exception exception = null;
    private JsonRPCtoRPCBridge bridge; /* who spawned this future */

    private SettableFuture<DOMRpcResult> jsonRPCFuture;
    private SettableFuture<String> uuidFuture;
    private String uuid;
    private final NormalizedNode<?, ?> input;
    private final SchemaPath type;
    private boolean pollingForResult;

    public JsonRPCDOMRpcResultFuture(
            SettableFuture<DOMRpcResult> jsonRPCFuture, SettableFuture<String> uuidFuture, JsonRPCtoRPCBridge bridge,
            final SchemaPath type, final NormalizedNode<?, ?> input
        ) {
        this.jsonRPCFuture = jsonRPCFuture;
        this.uuidFuture = uuidFuture;
        this.bridge = bridge;
        this.type = type;
        this.input = input;
        this.pollingForResult = false;
    }

    static CheckedFuture<DOMRpcResult, DOMRpcException> create(
        SettableFuture<DOMRpcResult> jsonRPCFuture, SettableFuture<String> uuidFuture, JsonRPCtoRPCBridge bridge,
        final SchemaPath type, final NormalizedNode<?, ?> input
        ) {
        return new JsonRPCDOMRpcResultFuture(jsonRPCFuture, uuidFuture, bridge, type, input);
    }

    public void setUuid(String uuid) {
        this.uuidFuture.set(uuid);
    }

    public SchemaPath getType() {
        return type;
    }

    public NormalizedNode<?, ?> getInput() {
        return this.input;
    } 

    boolean isPollingForResult() {
        return this.pollingForResult;
    }
    void startPollingForResult() {
        this.pollingForResult = true;
    }
    JsonObject formMetadata() {
        JsonObject result = new JsonObject();
        if (this.getUuid() != null) {
            result.add("async", new JsonPrimitive(this.getUuid().toString()));
        } else {
            result.add("async", new JsonPrimitive(UUID.randomUUID().toString()));
        }
        return result;
    }

    public UUID getUuid() {
        if (this.uuid == null) {
            return null;
        } else {
            return UUID.fromString(this.uuid);
        }
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        uuidFuture.cancel(true);
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
        this.uuid = null; /* cancel async ops */
        this.exception = e;
        if (!uuidFuture.isDone()) {
            uuidFuture.set(null);
        }
        jsonRPCFuture.set(null);
        return true; 
    }

    @Override
    public DOMRpcResult get() throws InterruptedException, ExecutionException {
        this.uuid = this.uuidFuture.get();
        if (this.uuid != null) {
            this.bridge.kick(this);
        }
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
        this.uuid = this.uuidFuture.get(timeout, unit);
        if (uuid != null) {
            this.bridge.kick(this);
        }
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
            this.uuid = this.uuidFuture.get();
            if (uuid != null) {
                this.bridge.kick(this);
            }
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
            this.uuid = this.uuidFuture.get(timeout, unit);
            if (uuid != null) {
                this.bridge.kick(this);
            }
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
