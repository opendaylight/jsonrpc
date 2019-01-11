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

import java.util.NoSuchElementException;

import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * This class holds common code for getting {@link RemoteOmShard} instances used
 * in {@link JsonRPCTx} and {@link JsonRPCDataBroker}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since May 6, 2018
 */
abstract class RemoteShardAware extends AbstractJsonRPCComponent implements AutoCloseable {
    private final LoadingCache<String, RemoteOmShard> shardCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, RemoteOmShard>() {
                @Override
                public RemoteOmShard load(String uri) throws Exception {
                    return transportFactory.createRequesterProxy(RemoteOmShard.class, uri);
                }
            });

    RemoteShardAware(SchemaContext schemaContext, TransportFactory transportFactory,
            HierarchicalEnumMap<JsonElement, DataType, String> pathMap, JsonConverter jsonConverter, Peer peer) {
        super(schemaContext, transportFactory, pathMap, jsonConverter ,peer);
    }

    protected RemoteOmShard getShard(final LogicalDatastoreType store, final JsonElement path) {
        return shardCache.getUnchecked(lookupEndPoint(store, path));
    }

    protected String lookupEndPoint(final LogicalDatastoreType store, final JsonElement path) {
        return pathMap.lookup(path, DataType.forDatastore(store)).orElseThrow(NoSuchElementException::new);
    }

    @Override
    public void close() {
        shardCache.asMap().values().stream().forEach(RemoteOmShard::close);
        shardCache.asMap().clear();
    }
}
