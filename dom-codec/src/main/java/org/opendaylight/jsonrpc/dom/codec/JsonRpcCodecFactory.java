/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import com.google.common.annotations.Beta;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

/**
 * Factory to create instances of various {@link Codec}s within given {@link EffectiveModelContext}. Created
 * {@link Codec} instances are cached for reuse and follow JSONRPC rule "be very tolerant at decoding, but be strict on
 * encoding" - which means that some JSON constructs differ from RFC7951. These differences are covered in
 * <a href="https://tools.ietf.org/html/draft-yang-json-rpc-03">Application of YANG Modeling to JSON RPCs for
 * Interoperability Purposes</a>.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 */
@Beta
public final class JsonRpcCodecFactory {
    private final EffectiveModelContext context;
    private final JsonRpcPathCodec pathCodec;

    private final LoadingCache<YangInstanceIdentifier, DataCodec> dataCodecCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<YangInstanceIdentifier, DataCodec>() {
                @Override
                public DataCodec load(YangInstanceIdentifier path) throws Exception {
                    return new DataCodec(context, path);
                }
            });

    private final LoadingCache<RpcDefinition, RpcIoCodecHolder> rpcCodecCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<RpcDefinition, RpcIoCodecHolder>() {
                @Override
                public RpcIoCodecHolder load(RpcDefinition path) throws Exception {
                    return new RpcIoCodecHolder(context, path);
                }
            });

    private final LoadingCache<NotificationDefinition, NotificationCodec> notifCodecCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<NotificationDefinition, NotificationCodec>() {
                @Override
                public NotificationCodec load(NotificationDefinition definition) throws Exception {
                    return NotificationCodec.create(context, definition);
                }
            });

    public JsonRpcCodecFactory(EffectiveModelContext context) {
        this.context = Objects.requireNonNull(context);
        pathCodec = JsonRpcPathCodec.create(context);
    }

    /**
     * Get {@link Codec} for RPC input.
     *
     * @param definition {@link RpcDefinition}
     * @return RPC input codec
     */
    public Codec<JsonElement, ContainerNode, IOException> rpcInputCodec(@NonNull RpcDefinition definition) {
        Objects.requireNonNull(definition);
        return rpcCodecCache.getUnchecked(definition).input();
    }

    /**
     * Get {@link Codec} for RPC output.
     *
     * @param definition {@link RpcDefinition}
     * @return RPC input codec
     */
    public Codec<JsonElement, ContainerNode, IOException> rpcOutputCodec(@NonNull RpcDefinition definition) {
        return rpcCodecCache.getUnchecked(definition).output();
    }

    /**
     * Get {@link Codec} to translate between JSONRPC notification encoded as {@link JsonElement} and
     * {@link DOMNotification}.
     *
     * @param definition schema definition of notification
     * @return notification codec
     */
    public Codec<JsonElement, DOMNotification, IOException> notificationCodec(
            @NonNull NotificationDefinition definition) {
        Objects.requireNonNull(definition);
        return notifCodecCache.getUnchecked(definition);
    }

    /**
     * Get {@link Codec} to translate between JSONRPC path and {@link YangInstanceIdentifier}.
     *
     * @return path codec
     */
    public Codec<JsonObject, YangInstanceIdentifier, RuntimeException> pathCodec() {
        return pathCodec;
    }

    /**
     * Get {@link Codec} to translate between JSONRPC data and {@link NormalizedNode}s.
     *
     * @param path path within data tree
     * @return data codec
     */
    public Codec<JsonElement, NormalizedNode, IOException> dataCodec(@NonNull YangInstanceIdentifier path) {
        Objects.requireNonNull(path);
        if (YangInstanceIdentifier.empty().equals(path)) {
            throw new IllegalArgumentException("Empty path is not supported by data codec");
        }
        return dataCodecCache.getUnchecked(path);
    }
}
