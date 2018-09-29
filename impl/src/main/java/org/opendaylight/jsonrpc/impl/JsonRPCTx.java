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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.JSONRPCArg;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.jsonrpc.model.TransactionListener;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
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

public class JsonRPCTx extends RemoteShardAware implements DOMDataTreeReadWriteTransaction, DOMDataTreeReadTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCTx.class);
    private static final JSONCodecFactorySupplier CODEC = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02;
    private final String deviceName;

    /* Transaction ID */
    private final Map<String, RemoteOmShard> endPointMap;
    private final Map<String, String> txIdMap;
    private final List<TransactionListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Instantiates a new ZMQ Bus Transaction.
     *
     * @param transportFactory used to create underlying transport connections
     * @param deviceName the bus om interface to use
     * @param pathMap shared instance of {@link HierarchicalEnumMap}
     * @param jsonConverter the conversion janitor instance
     * @param schemaContext the schema context
     */
    public JsonRPCTx(@Nonnull TransportFactory transportFactory, @Nonnull String deviceName,
            @Nonnull HierarchicalEnumMap<JsonElement, DataType, String> pathMap, @Nonnull JsonConverter jsonConverter,
            @Nonnull SchemaContext schemaContext) {
        super(schemaContext, transportFactory, pathMap, jsonConverter);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(deviceName), "Peer name is missing");
        this.deviceName = deviceName;
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
            return readFailure();
        }
        final RemoteOmShard omshard = getOmShard(store, arg.getPath());
        /* Read from the bus and adjust for BUS to ODL differences */
        final JsonObject rootJson;
        try {
            rootJson = jsonConverter.fromBus(path,
                    omshard.read(store2str(store2int(store)), deviceName, arg.getPath()));
        } catch (Exception e) {
            return readFailure(e);
        }

        if (rootJson == null) {
            return readFailure();
        }
        final NormalizedNodeResult result = new NormalizedNodeResult();
        DataNodeContainer tracker = schemaContext;
        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result)) {
            final Iterator<PathArgument> pathIterator = path.getPathArguments().iterator();
            while (pathIterator.hasNext()) {
                final PathArgument step = pathIterator.next();
                if (pathIterator.hasNext()) {
                    final DataSchemaNode nextNode = tracker.findDataChildByName(step.getNodeType()).get();
                    if (nextNode == null) {
                        LOG.error("cannot locate corresponding schema node {}", step.getNodeType().getLocalName());
                        return readFailure();
                    }
                    if (!DataNodeContainer.class.isInstance(nextNode)) {
                        LOG.error("corresponding schema node {} is neither list nor container",
                                step.getNodeType().getLocalName());
                        return readFailure();
                    }
                    /*
                     * List looks like a two path entry sequentially, so we need
                     * to skip one
                     */
                    if (!ListSchemaNode.class.isInstance(nextNode)) {
                        tracker = (DataNodeContainer) nextNode;
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
            throw new IllegalStateException("Failed to close NormalizedNodeStreamWriter", e);
        }
    }

    private FluentFuture<Optional<NormalizedNode<?, ?>>> readFailure(Exception ex) {
        return FluentFutures.immediateFailedFluentFuture(ex);
    }

    private FluentFuture<Optional<NormalizedNode<?, ?>>> readFailure() {
        return FluentFutures.immediateFluentFuture(Optional.empty());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public FluentFuture<Boolean> exists(LogicalDatastoreType store, YangInstanceIdentifier path) {
        final JSONRPCArg arg = jsonConverter.toBus(path, null);
        final RemoteOmShard omshard = getOmShard(store, arg.getPath());
        try {
            return FluentFutures.immediateBooleanFluentFuture(
                    omshard.exists(store2str(store2int(store)), deviceName, arg.getPath()));
        } catch (Exception e) {
            return FluentFutures.immediateFailedFluentFuture(e);
        }
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        final JSONRPCArg arg = jsonConverter.toBusWithStripControl(path, data, true);
        if (arg.getData() != null) {
            getOmShard(store, arg.getPath()).put(getTxId(lookupEndPoint(store, arg.getPath())),
                    store2str(store2int(store)), deviceName, arg.getPath(), arg.getData());
        }
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        final JSONRPCArg arg = jsonConverter.toBus(path, data);
        final RemoteOmShard omshard = getOmShard(store, arg.getPath());
        omshard.merge(getTxId(lookupEndPoint(store, arg.getPath())), store2str(store2int(store)), deviceName,
                arg.getPath(), arg.getData());
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        final JSONRPCArg arg = jsonConverter.toBus(path, null);
        final RemoteOmShard omshard = getOmShard(store, arg.getPath());
        omshard.delete(getTxId(lookupEndPoint(store, arg.getPath())), store2str(store2int(store)), deviceName,
                arg.getPath());
    }

    @Override
    public Object getIdentifier() {
        return this;
    }

    @Override
    public boolean cancel() {
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
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        listeners.forEach(txl -> txl.onSubmit(this));
        final AtomicBoolean result = new AtomicBoolean(true);
        endPointMap.entrySet()
                .forEach(entry -> result.set(result.get() && entry.getValue().commit(getTxId(entry.getKey()))));
        if (result.get()) {
            listeners.forEach(txListener -> txListener.onSuccess(this));
            return CommitInfo.emptyFluentFuture();
        } else {
            final Throwable failure = new TransactionCommitFailedException(
                    "Commit of transaction " + getIdentifier() + " failed");
            listeners.forEach(txListener -> txListener.onFailure(this, failure));
            return FluentFutures.immediateFailedFluentFuture(failure);
        }
    }

    AutoCloseable addCallback(TransactionListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
}
