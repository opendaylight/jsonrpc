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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.JSONRPCArg;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.MappingCheckedFuture;
import org.opendaylight.yangtools.util.concurrent.ExceptionMapper;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
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

@SuppressWarnings("deprecation")
public class JsonRPCTx extends AbstractJsonRPCComponent
        implements DOMDataReadWriteTransaction, DOMDataReadOnlyTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCTx.class);
    private final String deviceName;

    /* Transaction ID */
    private final Map<String, RemoteOmShard> endPointMap;
    private final Map<String, String> txIdMap;

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

    private String lookupEndPoint(final LogicalDatastoreType store, JsonElement path) {
        return pathMap.lookup(path, DataType.forDatastore(store)).orElse(null);
    }

    private RemoteOmShard getOmShard(final LogicalDatastoreType store, JsonElement path) {
        final String endpoint = lookupEndPoint(store, path);
        return endPointMap.computeIfAbsent(endpoint, ep -> {
            try {
                final String fixedEndpoint = Util.ensureRole(endpoint, SessionType.REQ);
                return transportFactory.createRequesterProxy(RemoteOmShard.class, fixedEndpoint);
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Provided URI is invalid", e);
            }
        });
    }

    private String getTxId(String endpoint) {
        return txIdMap.computeIfAbsent(endpoint, k -> endPointMap.get(k).txid());
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    // jsonParser may be null in the finally block below - this is OK but FB flags it.
    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final LogicalDatastoreType store,
            final YangInstanceIdentifier path) {
        final JSONRPCArg arg = jsonConverter.toBus(path, null);
        if (path.getPathArguments().isEmpty()) {
            return readFailure();
        }
        final RemoteOmShard omshard = getOmShard(store, arg.getPath());
        /* Read from the bus and adjust for BUS to ODL differences */
        JsonObject rootJson = null;
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
        JsonParserStream jsonParser = null;
        DataNodeContainer tracker = schemaContext;
        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result)) {
            final Iterator<PathArgument> pathIterator = path.getPathArguments().iterator();
            while (pathIterator.hasNext()) {
                final PathArgument step = pathIterator.next();
                if (pathIterator.hasNext()) {
                    final DataSchemaNode nextNode = tracker.getDataChildByName(step.getNodeType());
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

            jsonParser = JsonParserStream.create(streamWriter, schemaContext, (SchemaNode) tracker);
            /*
             * Kludge - we convert to string so that the StringReader can
             * consume it, we need to replace this with a native translator into
             * NormalizedNode
             */
            try {
                jsonParser.parse(new JsonReader(new StringReader(rootJson.toString())));
            } catch (IllegalArgumentException e) {
                LOG.error("Failed to parse read data {}", rootJson.toString());
                return readFailure(e);
            }
            final ListenableFuture<Optional<NormalizedNode<?, ?>>> future = Futures
                    .immediateFuture(Optional.<NormalizedNode<?, ?>>of(result.getResult()));
            switch (store) {
                case CONFIGURATION:
                case OPERATIONAL:
                    return MappingCheckedFuture.create(future, ReadFailedException.MAPPER);
                default:
                    throw new IllegalArgumentException(String.format(
                        "%s, Cannot read data %s for %s datastore, unknown datastore type", deviceName, path, store));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close NormalizedNodeStreamWriter", e);
        } finally {
            Util.closeNullableWithExceptionCallback(jsonParser, e -> LOG.warn("Failed to close JsonParser", e));
        }
    }

    private CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readFailure(Exception ex) {
        return MappingCheckedFuture.create(Futures.immediateFailedFuture(ex), ReadFailedException.MAPPER);
    }

    private CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readFailure() {
        return MappingCheckedFuture.create(Futures.immediateFuture(Optional.<NormalizedNode<?, ?>>absent()),
                ReadFailedException.MAPPER);
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public CheckedFuture<Boolean, ReadFailedException> exists(LogicalDatastoreType store, YangInstanceIdentifier path) {
        final JSONRPCArg arg = jsonConverter.toBus(path, null);
        final RemoteOmShard omshard = getOmShard(store, arg.getPath());
        try {
            return Futures.immediateCheckedFuture(omshard.exists(store2str(store2int(store)),
                    deviceName, arg.getPath()));
        } catch (Exception e) {
            return MappingCheckedFuture.create(Futures.immediateFailedFuture(e), ReadFailedException.MAPPER);
        }
    }

    @Override
    public void close() {
        endPointMap.entrySet().forEach(e -> Util.closeNullableWithExceptionCallback(e.getValue(),
            t -> LOG.warn("Failed to close RemoteOmShard proxy", t)));
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        final JSONRPCArg arg = jsonConverter.toBusWithStripControl(path, data, true);
        if (arg.getData() != null) {
            /* ODL supplies a null arg to create an entry before setting it */
            RemoteOmShard omshard = getOmShard(store, arg.getPath());
            /* this is ugly - extra lookup, needs fixing on another pass */
            omshard.put(getTxId(lookupEndPoint(store, arg.getPath())), store2str(store2int(store)), deviceName,
                    arg.getPath(), arg.getData());
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
        omshard.delete(getTxId(lookupEndPoint(store, arg.getPath())),
                store2str(store2int(store)), deviceName, arg.getPath());
    }

    @Override
    public Object getIdentifier() {
        return this;
    }

    @Override
    public boolean cancel() {
        boolean result = true;
        for (Map.Entry<String, RemoteOmShard> entry : this.endPointMap.entrySet()) {
            RemoteOmShard omshard = this.endPointMap.get(entry.getKey());
            if (getTxId(entry.getKey()) != null) {
                /*
                 * We never allocated a txid, so no need to send message to om.
                 */
                result &= omshard.cancel(getTxId(entry.getKey()));
            }
        }
        return result;
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        return MappingCheckedFuture.create(commit().transform(ignored -> null, MoreExecutors.directExecutor()),
            new ExceptionMapper<TransactionCommitFailedException>("commit", TransactionCommitFailedException.class) {
                @Override
                protected TransactionCommitFailedException newWithCause(String message, Throwable cause) {
                    return new TransactionCommitFailedException(message, cause);
                }
            });
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        final AtomicBoolean result = new AtomicBoolean(true);
        endPointMap.entrySet().forEach(entry -> {
            result.set(result.get() && entry.getValue().commit(getTxId(entry.getKey())));
        });

        if (result.get()) {
            return CommitInfo.emptyFluentFuture();
        }

        return FluentFuture.from(Futures.immediateFailedFuture(new TransactionCommitFailedException(
                "Commit of transaction " + getIdentifier() + " failed")));
    }
}
