/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.jsonrpc.bus.api.SessionType;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpoints;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Utility class.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public final class Util {
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
     * Transform JSON RPC 2.0 representation of a data store into
     * {@link LogicalDatastoreType}.
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
     * Transform string representation of a data store into
     * {@link LogicalDatastoreType}.
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
            Preconditions.checkNotNull(s, ERR_UNRECOGNIZED_STORE, store);
            return s;
        }
    }

    /**
     * Transform instance of {@link LogicalDatastoreType} to JSON RPC 2.0
     * representation of a data store.
     *
     * @param store Logical Data Store
     * @return 0 for config, 1 for operational
     */
    public static int store2int(@Nonnull final LogicalDatastoreType store) {
        Preconditions.checkNotNull(store);
        final Integer ldt = STORE_MAP.inverse().get(store);
        Preconditions.checkNotNull(ldt, ERR_UNRECOGNIZED_STORE, store);
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
     * @throws Exception if invocation of {@link AutoCloseable#close()} throws
     *             exception
     */
    public static void closeNullable(@Nullable AutoCloseable closeable) throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    /**
     * Ensures that given URI will contain proper 'role' query parameter as
     * required by {@link TransportFactory}'s method.
     *
     * @param inUri URI to check(and eventually fix) for presence of 'role'
     *            parameter
     * @param role {@link SessionType} to use
     * @return URI which will contain 'role' query parameter as requested
     */
    public static String ensureRole(String inUri, SessionType role) {
        int idx = inUri.indexOf('?');
        if (idx == -1) {
            return inUri + "?" + "role=" + role.name();
        } else {
            return inUri.substring(0, idx + 1) + org.opendaylight.jsonrpc.bus.messagelib.Util
                    .replaceParam(inUri.substring(idx + 1), "role", role.name());
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
            Collection<? extends Endpoint> endpoints, DataType key) {
        endpoints.stream().filter(p -> p != null && p.getEndpointUri() != null).forEach(
            ep -> pathMap.put(GSON.fromJson(ep.getPath(), JsonObject.class), key, ep.getEndpointUri().getValue()));
    }

    /**
     * Utility method to close {@link AutoCloseable} with eventual exception
     * callback.
     *
     * @param closeable {@link AutoCloseable} instance to close, can be null
     * @param callback Callback to be invoked when exception occur, must not be
     *            null
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void closeNullableWithExceptionCallback(@Nullable AutoCloseable closeable,
            @Nonnull Consumer<Exception> callback) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                callback.accept(e);
            }
        }
    }

    /**
     * Create a Binding Independent path identifier for a name.
     *
     * @param name - name to create the Bi identifier for
     * @return Bi identifier
     */
    public static YangInstanceIdentifier createBiPath(final String name) {
        final YangInstanceIdentifier.InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        builder.node(Config.QNAME).node(ConfiguredEndpoints.QNAME).nodeWithKey(ConfiguredEndpoints.QNAME,
                QName.create(ConfiguredEndpoints.QNAME.getNamespace(), ConfiguredEndpoints.QNAME.getRevision(), "name"),
                name);
        return builder.build();
    }

    static Optional<Module> findModuleWithLatestRevision(SchemaContext schemaContext, String name) {
        // findModules is guaranteed to return latest revision first
        return schemaContext.findModules(name).stream().findFirst();
    }
}
