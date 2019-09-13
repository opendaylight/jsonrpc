/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonElement;

import java.util.function.Supplier;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * This class holds common code for getting {@link RemoteOmShard} instances used
 * in {@link JsonRPCTx} and {@link JsonRPCDataBroker}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since May 6, 2018
 */
abstract class RemoteShardAware extends AbstractJsonRPCComponent implements AutoCloseable {
    private static final String SHARD_NOT_AVAILABLE = "Datastore mapping does not exists "
            + "for store '%s'at path '%s'.Make sure that requested path is within configured data endpoints "
            + "or governance is aware of such path.";
    private final LoadingCache<String, RemoteOmShard> shardCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, RemoteOmShard>() {
                @Override
                public RemoteOmShard load(String uri) throws Exception {
                    return transportFactory.createRequesterProxy(RemoteOmShard.class, uri,
                            TransportFactory.DEFAULT_TIMEOUT);
                }
            });

    RemoteShardAware(SchemaContext schemaContext, TransportFactory transportFactory,
            HierarchicalEnumMap<JsonElement, DataType, String> pathMap, JsonConverter jsonConverter) {
        super(schemaContext, transportFactory, pathMap, jsonConverter);
    }

    protected RemoteOmShard getShard(final LogicalDatastoreType store, final JsonElement path) {
        return shardCache.getUnchecked(lookupEndPoint(store, path));
    }

    protected String lookupEndPoint(final LogicalDatastoreType store, final JsonElement path) {
        return pathMap.lookup(path, DataType.forDatastore(store)).orElseThrow(shardNotAvailable(store, path));
    }

    /**
     * Provide human readable error when mapping for requested datastore/path does not exists.
     *
     * @param store datastore that is subject to current data operation
     * @param path that is subject to current data operation
     * @return {@link Supplier} that provides {@link IllegalArgumentException}
     */
    private Supplier<IllegalArgumentException> shardNotAvailable(final LogicalDatastoreType store,
            final JsonElement path) {
        return () -> new IllegalArgumentException(String.format(SHARD_NOT_AVAILABLE, store, path));
    }

    @Override
    public void close() {
        shardCache.asMap().values().stream().forEach(RemoteOmShard::close);
        shardCache.asMap().clear();
    }
}
