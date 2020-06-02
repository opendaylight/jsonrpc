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
import static org.opendaylight.yangtools.util.concurrent.FluentFutures.immediateFluentFuture;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FluentFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.JSONRPCArg;
import org.opendaylight.jsonrpc.model.JsonRpcTransactionFacade;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.jsonrpc.model.TransactionListener;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRPCTx extends RemoteShardAware implements JsonRpcTransactionFacade {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCTx.class);
    private static final Function<String, RpcError> ERROR_MAPPER = msg -> RpcResultBuilder
            .newError(ErrorType.APPLICATION, "commit", msg);
    private static final FluentFuture<Optional<NormalizedNode<?, ?>>> NO_DATA = FluentFutures
            .immediateFluentFuture(Optional.empty());
    /* Keep track of TX id to given endpoint (key is endpoint, value is TX ID) */
    private final Map<String, String> txIdMap;
    private final List<TransactionListener> listeners = new CopyOnWriteArrayList<>();
    private final JsonRpcPathCodec pathCodec;

    /**
     * Instantiates a new JSONRPC Transaction.
     *
     * @param transportFactory used to create underlying transport connections
     * @param peer remote peer
     * @param pathMap shared instance of {@link HierarchicalEnumMap}
     * @param jsonConverter the conversion janitor instance
     * @param schemaContext the schema context
     */
    public JsonRPCTx(@NonNull TransportFactory transportFactory, @NonNull Peer peer,
            @NonNull HierarchicalEnumMap<JsonElement, DataType, String> pathMap, @NonNull JsonConverter jsonConverter,
            @NonNull SchemaContext schemaContext) {
        super(schemaContext, transportFactory, pathMap, jsonConverter, peer);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(peer.getName()), "Peer name is missing");
        this.txIdMap = new HashMap<>();
        this.pathCodec = JsonRpcPathCodec.create(schemaContext);
    }

    private <T> T withRemoteShard(LogicalDatastoreType store, JsonElement path,
            Function<RemoteOmShard, T> mappingFunction) {
        try (RemoteOmShard shard = getShard(store, path)) {
            return mappingFunction.apply(shard);
        }
    }

    /*
     * Get cached TX id or allocate new one from corresponding remote shard mapped to given store and path
     */
    private String getTxId(LogicalDatastoreType store, JsonElement path) {
        return txIdMap.computeIfAbsent(lookupEndPoint(store, path),
            k -> withRemoteShard(store, path, RemoteOmShard::txid));
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public FluentFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
        if (path.getPathArguments().isEmpty()) {
            return NO_DATA;
        }
        final JsonObject jsonPath = pathCodec.serialize(path);
        try (RemoteOmShard shard = getShard(store, jsonPath)) {
            final JsonElement data = jsonConverter.fromBus(path,
                    shard.read(store2str(store2int(store)), peer.getName(), jsonPath));
            if (data == null) {
                return NO_DATA;
            }
            return immediateFluentFuture(Optional.of(jsonConverter.jsonElementToNormalizedNode(data, path, false)));
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public FluentFuture<Boolean> exists(LogicalDatastoreType store, YangInstanceIdentifier path) {
        final JsonObject jsonPath = pathCodec.serialize(path);
        try (RemoteOmShard omshard = getShard(store, jsonPath)) {
            return FluentFutures.immediateBooleanFluentFuture(
                    omshard.exists(store2str(store2int(store)), peer.getName(), jsonPath));
        } catch (Exception e) {
            return FluentFutures.immediateFailedFluentFuture(ReadFailedException.MAPPER.apply(e));
        }
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        final JSONRPCArg arg = jsonConverter.toBusWithStripControl(path, data, true);
        if (arg.getData() != null) {
            try (RemoteOmShard omshard = getShard(store, arg.getPath())) {
                omshard.put(getTxId(store, arg.getPath()), store2str(store2int(store)), peer.getName(), arg.getPath(),
                        arg.getData());
            }
        }
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        final JSONRPCArg arg = jsonConverter.toBus(path, data);
        try (RemoteOmShard omshard = getShard(store, arg.getPath())) {
            omshard.merge(getTxId(store, arg.getPath()), store2str(store2int(store)), peer.getName(), arg.getPath(),
                    arg.getData());
        }
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        final JsonObject jsonPath = pathCodec.serialize(path);
        try (RemoteOmShard omshard = getShard(store, jsonPath)) {
            omshard.delete(getTxId(store, jsonPath), store2str(store2int(store)), peer.getName(), jsonPath);
        }
    }

    @Override
    public Object getIdentifier() {
        return super.hashCode();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    public boolean cancel() {
        try {
            boolean result = true;
            for (Map.Entry<String, String> entry : txIdMap.entrySet()) {
                try (RemoteOmShard shard = getShard(entry.getKey())) {
                    result &= shard.cancel(entry.getValue());
                }
            }
            txIdMap.clear();
            listeners.forEach(listener -> listener.onCancel(this));
            return result;
        } catch (Exception e) {
            LOG.error("Unable to cancel transaction", e);
            return false;
        }
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        listeners.forEach(txl -> txl.onSubmit(this));
        boolean result = true;
        final List<String> errors = new ArrayList<>();

        for (Map.Entry<String, String> entry : txIdMap.entrySet()) {
            try (RemoteOmShard shard = getShard(entry.getKey())) {
                if (!shard.commit(entry.getValue())) {
                    result = false;
                    LOG.debug("Commit of {} failed, requesting more info", entry.getValue());
                    errors.addAll(shard.error(entry.getValue()));
                }
            }
        }
        txIdMap.clear();
        if (result) {
            listeners.forEach(txListener -> txListener.onSuccess(this));
            return CommitInfo.emptyFluentFuture();
        } else {
            final Throwable failure = new TransactionCommitFailedException(
                    "Commit of transaction " + getIdentifier() + " failed",
                    errors.stream().map(ERROR_MAPPER).toArray(size -> new RpcError[size]));
            listeners.forEach(txListener -> txListener.onFailure(this, failure));
            return FluentFutures.immediateFailedFluentFuture(failure);
        }
    }

    @Override
    public AutoCloseable addCallback(TransactionListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getIdentifier());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof JsonRpcTransactionFacade)) {
            return false;
        }
        JsonRpcTransactionFacade other = (JsonRpcTransactionFacade) obj;
        return getIdentifier().equals(other.getIdentifier());
    }
}
