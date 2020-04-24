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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FluentFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRPCTx extends RemoteShardAware implements JsonRpcTransactionFacade {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCTx.class);
    private static final JSONCodecFactorySupplier CODEC = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02;
    private static final Function<String, RpcError> ERROR_MAPPER = msg -> RpcResultBuilder
            .newError(ErrorType.APPLICATION, "commit", msg);
    private static final FluentFuture<Optional<NormalizedNode<?, ?>>> NO_DATA = FluentFutures
            .immediateFluentFuture(Optional.empty());
    /* Transaction ID */
    private final Map<String, RemoteOmShard> endPointMap;
    private final Map<String, String> txIdMap;
    private final List<TransactionListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Instantiates a new ZMQ Bus Transaction.
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
        this.endPointMap = new HashMap<>();
        this.txIdMap = new HashMap<>();
        /*
         * Instantiate our "for real" bus interface. This is a simplification -
         * it assumes that the "ownership" of a device does not change across
         * the device tree. That may not be the case as a device is entitled to
         * be sharded, same as we are. We, however, will cross this bridge when
         * we cross it and we may end up crossing it on the bus side via proxy
         * functionality in the device tree implementation.
         */
    }

    private RemoteOmShard getOmShard(final LogicalDatastoreType store, JsonElement path) {
        return endPointMap.computeIfAbsent(lookupEndPoint(store, path), shard -> getShard(store, path));
    }

    private String getTxId(String endpoint) {
        return txIdMap.computeIfAbsent(endpoint, k -> endPointMap.get(k).txid());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    // jsonParser may be null in the finally block below - this is OK but FB flags it.
    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    public FluentFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
        final JSONRPCArg arg = jsonConverter.toBus(path, null);
        if (path.getPathArguments().isEmpty()) {
            return NO_DATA;
        }
        final NormalizedNodeResult result = new NormalizedNodeResult();
        DataNodeContainer tracker = schemaContext;
        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result)) {
            final RemoteOmShard omshard = getOmShard(store, arg.getPath());
            /* Read from the bus and adjust for BUS to ODL differences */
            final JsonObject rootJson = jsonConverter.fromBus(path,
                    omshard.read(store2str(store2int(store)), peer.getName(), arg.getPath()));
            if (rootJson == null) {
                return NO_DATA;
            }
            final Iterator<PathArgument> pathIterator = path.getPathArguments().iterator();
            while (pathIterator.hasNext()) {
                final PathArgument step = pathIterator.next();
                if (pathIterator.hasNext()) {
                    final Optional<DataSchemaNode> nextNode = tracker.findDataChildByName(step.getNodeType());
                    if (!nextNode.isPresent()) {
                        return readFailure("Cannot locate corresponding schema node "
                                + step.getNodeType().getLocalName());
                    }
                    if (!DataNodeContainer.class.isInstance(nextNode.get())) {
                        return readFailure("Corresponding schema node " + step.getNodeType().getLocalName()
                                + " is neither list nor container");
                    }
                    /*
                     * List looks like a two path entry sequentially, so we need
                     * to skip one
                     */
                    if (!ListSchemaNode.class.isInstance(nextNode)) {
                        tracker = (DataNodeContainer) nextNode.get();
                    }
                }
            }
            try (JsonParserStream jsonParser = JsonParserStream.create(streamWriter, CODEC.getShared(schemaContext),
                    (SchemaNode) tracker)) {
                /*
                 * Kludge - we convert to string so that the StringReader can
                 * consume it, we need to replace this with a native translator into
                 * NormalizedNode
                 */
                jsonParser.parse(new JsonReader(new StringReader(rootJson.toString())));
                return FluentFutures.immediateFluentFuture(Optional.of(result.getResult()));

            }
        } catch (IOException e) {
            return readFailure("I/O error while reading data at path" + path, e);
        } catch (Exception e) {
            return readFailure("Unable to read data at path " + path, e);
        }
    }

    private FluentFuture<Optional<NormalizedNode<?, ?>>> readFailure(String message, Exception ex) {
        return FluentFutures.immediateFailedFluentFuture(new ReadFailedException(message, ex));
    }

    private FluentFuture<Optional<NormalizedNode<?, ?>>> readFailure(String message) {
        return readFailure(message, null);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public FluentFuture<Boolean> exists(LogicalDatastoreType store, YangInstanceIdentifier path) {
        try {
            final JSONRPCArg arg = jsonConverter.toBus(path, null);
            final RemoteOmShard omshard = getOmShard(store, arg.getPath());
            return FluentFutures.immediateBooleanFluentFuture(
                    omshard.exists(store2str(store2int(store)), peer.getName(), arg.getPath()));
        } catch (Exception e) {
            return FluentFutures.immediateFailedFluentFuture(ReadFailedException.MAPPER.apply(e));
        }
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        final JSONRPCArg arg = jsonConverter.toBusWithStripControl(path, data, true);
        if (arg.getData() != null) {
            getOmShard(store, arg.getPath()).put(getTxId(lookupEndPoint(store, arg.getPath())),
                    store2str(store2int(store)), peer.getName(), arg.getPath(), arg.getData());
        }
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        final JSONRPCArg arg = jsonConverter.toBus(path, data);
        final RemoteOmShard omshard = getOmShard(store, arg.getPath());
        omshard.merge(getTxId(lookupEndPoint(store, arg.getPath())), store2str(store2int(store)), peer.getName(),
                arg.getPath(), arg.getData());
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        final JSONRPCArg arg = jsonConverter.toBus(path, null);
        final RemoteOmShard omshard = getOmShard(store, arg.getPath());
        omshard.delete(getTxId(lookupEndPoint(store, arg.getPath())), store2str(store2int(store)), peer.getName(),
                arg.getPath());
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
            for (Map.Entry<String, RemoteOmShard> entry : endPointMap.entrySet()) {
                RemoteOmShard omshard = endPointMap.get(entry.getKey());
                if (getTxId(entry.getKey()) != null) {
                    /*
                     * We never allocated a txid, so no need to send message to om.
                     */
                    result &= omshard.cancel(getTxId(entry.getKey()));
                }
            }
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
        for (Entry<String, RemoteOmShard> entry : endPointMap.entrySet()) {
            final String txid = getTxId(entry.getKey());
            if (!entry.getValue().commit(txid)) {
                result = false;
                LOG.debug("Commit of {} failed, requesting more info", txid);
                errors.addAll(entry.getValue().error(txid));
            }
        }
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
