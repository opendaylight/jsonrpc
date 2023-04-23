/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpoints;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public final class Util {
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);
    private static final String ERR_UNRECOGNIZED_STORE = "Unrecognized store value %s";
    private static final Gson GSON = new Gson();
    private static final BiMap<Integer, LogicalDatastoreType> STORE_MAP = ImmutableBiMap
            .<Integer, LogicalDatastoreType>builder()
            .put(0, LogicalDatastoreType.CONFIGURATION)
            .put(1, LogicalDatastoreType.OPERATIONAL)
            .build();

    private static final BiMap<String, Integer> STORE_STR_MAP = ImmutableBiMap.<String, Integer>builder()
            .put("config", 0)
            .put("operational", 1)
            .build();

    private Util() {
        // utility-class constructor
    }

    /**
     * Transform JSON RPC 2.0 representation of a data store into {@link LogicalDatastoreType}.
     *
     * @param store data store in form of number
     * @return {@link LogicalDatastoreType}.
     */
    public static LogicalDatastoreType int2store(final int store) {
        final LogicalDatastoreType ldt = STORE_MAP.get(store);
        Preconditions.checkNotNull(ldt, ERR_UNRECOGNIZED_STORE, store);
        return ldt;
    }

    /**
     * Transform string representation of a data store into {@link LogicalDatastoreType}.
     *
     * @param store Logical Data Store
     * @return 0 for "config", 1 for "operational"
     */
    public static int store2int(final String store) {
        try {
            /* Integer quoted as string - common JSON bugbear */
            return Integer.parseInt(store);
        } catch (NumberFormatException e) {
            /* String representation of the datastore */
            final Integer s = STORE_STR_MAP.get(store);
            Objects.requireNonNull(s, () -> String.format(ERR_UNRECOGNIZED_STORE, store));
            return s;
        }
    }

    /**
     * Transform instance of {@link LogicalDatastoreType} to JSON RPC 2.0 representation of a data store.
     *
     * @param store Logical Data Store
     * @return 0 for config, 1 for operational
     */
    public static int store2int(@NonNull final LogicalDatastoreType store) {
        Objects.requireNonNull(store);
        final Integer ldt = STORE_MAP.inverse().get(store);
        Objects.requireNonNull(ldt, () -> String.format(ERR_UNRECOGNIZED_STORE, store));
        return ldt;
    }

    /**
     * Transform {@link LogicalDatastoreType} into string representation.
     *
     * @param store Logical Data Store
     * @return "config" for 0, "operational" for 1
     */
    public static String store2str(final int store) {
        final String s = STORE_STR_MAP.inverse().get(store);
        Preconditions.checkNotNull(s, ERR_UNRECOGNIZED_STORE, store);
        return s;
    }

    /**
     * Utility method to reduce repeated null checks.
     *
     * @param closeable an {@link AutoCloseable} to close
     * @throws Exception if invocation of {@link AutoCloseable#close()} throws exception
     */
    public static void closeNullable(@Nullable AutoCloseable closeable) throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    /**
     * Populates {@link HierarchicalEnumMap} entries from list of endpoints.
     *
     * @param pathMap {@link HierarchicalEnumMap} to populate
     * @param endpoints list of endpoints
     * @param key {@link DataType} of entries to populate
     */
    public static void populateFromEndpointList(HierarchicalEnumMap<JsonElement, DataType, String> pathMap,
            @Nullable Collection<? extends Endpoint> endpoints, DataType key) {
        Optional.ofNullable(endpoints)
                .orElse(Collections.emptyList())
                .stream()
                .filter(p -> p != null && p.getEndpointUri() != null)
                .forEach(ep -> pathMap.put(GSON.fromJson(ep.getPath(), JsonObject.class), key,
                        ep.getEndpointUri().getValue()));
    }

    /**
     * Attempt to close provided {@link AutoCloseable} instance and log error if {@link Exception} is thrown.
     *
     * @param closeable instance of {@link AutoCloseable} to close, can be null.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void closeAndLogOnError(@Nullable AutoCloseable closeable) {
        try {
            closeNullable(closeable);
        } catch (Exception e) {
            LOG.error("Fail to close {}", closeable, e);
        }
    }

    /**
     * Remove entry from {@link Map} using provided key and call {@link AutoCloseable#close()} if entry was found (and
     * removed).
     *
     * @param <K> type key
     * @param <S> type of value
     * @param map {@link Map} to remove entry from
     * @param key entry key
     */
    public static <K, S extends AutoCloseable> void removeFromMapAndClose(Map<K, S> map, K key) {
        Optional.ofNullable(map.remove(key)).ifPresent(Util::closeAndLogOnError);
    }

    /**
     * Create a Binding Independent path identifier for a name.
     *
     * @param name - name to create the Bi identifier for
     * @return Bi identifier
     */
    public static YangInstanceIdentifier createBiPath(final String name) {
        final YangInstanceIdentifier.InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        builder.node(Config.QNAME)
                .node(ConfiguredEndpoints.QNAME)
                .nodeWithKey(ConfiguredEndpoints.QNAME, QName.create(ConfiguredEndpoints.QNAME.getNamespace(),
                        ConfiguredEndpoints.QNAME.getRevision(), "name"), name);
        return builder.build();
    }

    @VisibleForTesting
    static boolean isNullableTrue(@Nullable Boolean condition) {
        return condition != null && condition;
    }

    /**
     * Strip query parameters off URI.
     *
     * @param baseUri base URI to strip query parameters from
     * @return stripped URI
     */
    @VisibleForTesting
    static String stripUri(String baseUri) {
        try {
            final URI uri = new URI(baseUri);
            final StringBuilder sb = new StringBuilder();
            sb.append(uri.getScheme());
            sb.append("://");
            sb.append(uri.getHost());
            if (uri.getPort() != -1) {
                sb.append(':');
                sb.append(uri.getPort());
            }
            sb.append(uri.getPath());
            return sb.toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Find subtype of {@link SchemaNode} such as {@link NotificationDefinition} or {@link RpcDefinition} in
     * {@link SchemaContext} based on given name and {@link Function}.Name can be prefixed by module name.
     *
     * @param schemaContext {@link SchemaContext}
     * @param name name of RPC method
     * @param mapper {@link Function} that "extracts" {@link SchemaNode} subtype from {@link Module}
     * @return {@link Optional} of {@link NotificationDefinition}.
     */
    public static <T extends SchemaNode> Optional<T> findNode(SchemaContext schemaContext, String name,
            Function<Module, Collection<T>> mapper) {
        if (name.indexOf(':') != -1) {
            final String[] parts = name.split(":");
            final Optional<? extends Module> modOpt = findModule(schemaContext, parts[0]);
            if (modOpt.isEmpty()) {
                return Optional.empty();
            } else {
                return findNode(mapper.apply(modOpt.orElseThrow()).stream(), parts[1]);
            }
        } else {
            return findNode(schemaContext.getModules().stream().flatMap(m -> mapper.apply(m).stream()), name);
        }
    }

    private static <T extends SchemaNode> Optional<T> findNode(Stream<T> stream, String name) {
        return stream.filter(r -> r.getQName().getLocalName().equals(name)).findFirst();
    }

    private static Optional<? extends Module> findModule(SchemaContext schemaContext, String name) {
        return schemaContext.getModules().stream().filter(m -> m.getName().equals(name)).findFirst();
    }

    public static LogicalDatastoreType storeFromString(String str) {
        try {
            return int2store(Integer.parseInt(str));
        } catch (NumberFormatException e) {
            return int2store(STORE_STR_MAP.get(str));
        }
    }

    public static boolean supportInbandModels(Peer peer) {
        return (peer.getModules() != null && peer.getModules().size() == 1
                && peer.getModules().iterator().next().getValue().startsWith("jsonrpc-inband-models"));
    }
}
