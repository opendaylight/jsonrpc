/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others. All rights reserved.
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.jsonrpc.impl;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Simplistic implementation of CheckedFuture to be returned by the RPC implementation.
 *
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
@SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
final class JsonRPCDOMRpcResultFuture extends AbstractFuture<DOMRpcResult> {
    private volatile Exception exception = null;
    private final Consumer<JsonRPCDOMRpcResultFuture> consumer; /* who spawned this future */
    private final SettableFuture<DOMRpcResult> jsonRPCFuture;
    private final SettableFuture<String> uuidFuture;
    private String uuid;
    private final NormalizedNode<?, ?> input;
    private final SchemaPath type;
    private boolean pollingForResult;

    JsonRPCDOMRpcResultFuture(SettableFuture<DOMRpcResult> jsonRPCFuture, SettableFuture<String> uuidFuture,
            Consumer<JsonRPCDOMRpcResultFuture> consumer, final SchemaPath type, final NormalizedNode<?, ?> input) {
        this.jsonRPCFuture = jsonRPCFuture;
        this.uuidFuture = uuidFuture;
        this.consumer = consumer;
        this.type = type;
        this.input = input;
        this.pollingForResult = false;
    }

    static Future<DOMRpcResult> create(SettableFuture<DOMRpcResult> jsonRPCFuture,
            SettableFuture<String> uuidFuture, Consumer<JsonRPCDOMRpcResultFuture> consumer, final SchemaPath type,
            final NormalizedNode<?, ?> input) {
        return new JsonRPCDOMRpcResultFuture(jsonRPCFuture, uuidFuture, consumer, type, input);
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
        final JsonObject result = new JsonObject();
        if (uuid == null) {
            result.add("async", new JsonPrimitive(UUID.randomUUID().toString()));
        } else {
            result.add("async", new JsonPrimitive(UUID.fromString(uuid).toString()));
        }
        return result;
    }

    public UUID getUuid() {
        return uuid == null ? null : UUID.fromString(uuid);
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

    @SuppressFBWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
    @Override
    public boolean set(@Nullable DOMRpcResult value) {
        return jsonRPCFuture.set(value);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
    public boolean setException(Exception ex) {
        this.uuid = null; /* cancel async ops */
        this.exception = ex;
        if (!uuidFuture.isDone()) {
            uuidFuture.set(null);
        }
        jsonRPCFuture.set(null);
        return true;
    }

    @Override
    public DOMRpcResult get() throws InterruptedException, ExecutionException {
        uuid = this.uuidFuture.get();
        if (uuid != null) {
            consumer.accept(this);
        }
        final DOMRpcResult result =  jsonRPCFuture.get();
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
            this.consumer.accept(this);
        }
        DOMRpcResult result =  jsonRPCFuture.get(timeout, unit);
        if (this.exception == null) {
            return result;
        } else {
            throw new ExecutionException(this.exception);
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
