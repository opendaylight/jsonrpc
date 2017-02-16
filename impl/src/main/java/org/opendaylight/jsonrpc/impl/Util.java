/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.jsonrpc.bus.messagelib.EndpointRole;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Endpoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpoints;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.stream.ForwardingNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Utility class.
 * 
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class Util {
    private static final String ERR_UNRECOGNIZED_STORE = "Unrecognized store value %s";
    private static final Gson GSON = new Gson();
    private static final BiMap<Integer, LogicalDatastoreType> STORE_MAP = ImmutableBiMap
            .<Integer, LogicalDatastoreType>builder().put(0, LogicalDatastoreType.CONFIGURATION)
            .put(1, LogicalDatastoreType.OPERATIONAL).build();

    private static final BiMap<String, Integer> STORE_STR_MAP = ImmutableBiMap.<String, Integer>builder()
            .put("config", 0).put("operational", 1).build();

    private Util() {
        // utility-class constructor
    }

    /**
     * Transform JSON RPC 2.0 representation of a datastore into
     * {@link LogicalDatastoreType}
     *
     * @return 0 for CONFIGURATION, 1 for OPERATIONAL
     */
    public static LogicalDatastoreType int2store(final int store) {
        final LogicalDatastoreType ldt = STORE_MAP.get(store);
        Preconditions.checkNotNull(ldt, ERR_UNRECOGNIZED_STORE, store);
        return ldt;
    }

    /**
     * Transform string representation of a datastore into
     * {@link LogicalDatastoreType}
     *
     * @return 0 for "config", 1 for "operational"
     */
    public static int store2int(final String store) {
        final Integer s = STORE_STR_MAP.get(store);
        Preconditions.checkNotNull(s, ERR_UNRECOGNIZED_STORE, store);
        return s;
    }

    /**
     * Transform {@link LogicalDatastoreType} into string representation
     *
     * @return "config" for 0, "operational" for 1
     */
    public static String store2str(final int store) {
        final String s = STORE_STR_MAP.inverse().get(store);
        Preconditions.checkNotNull(s, ERR_UNRECOGNIZED_STORE, store);
        return s;
    }

    /**
     * Transform instance of {@link LogicalDatastoreType} to JSON RPC 2.0
     * representation of a datastore
     */
    public static int store2int(@Nonnull final LogicalDatastoreType store) {
        Preconditions.checkNotNull(store);
        final Integer ldt = STORE_MAP.inverse().get(store);
        Preconditions.checkNotNull(ldt, ERR_UNRECOGNIZED_STORE, store);
        return ldt;
    }

    /**
     * Utility method to reduce repeated null checks
     */
    public static void closeNullable(@Nullable AutoCloseable closeable) throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    /**
     * Allows to intercept anyXmlNode in stream and handle it
     */
    public static NormalizedNodeStreamWriter wrapWithAnyXmlNullValueCallBack(NormalizedNodeStreamWriter delegate) {
        return new ForwardingNormalizedNodeStreamWriter() {
            @Override
            public void anyxmlNode(NodeIdentifier name, Object value) throws IOException {
                if (value == null) {
                    throw new IllegalArgumentException("NULL anyxml value encountered at " + name);
                }
                super.anyxmlNode(name, value);
            }

            @Override
            protected NormalizedNodeStreamWriter delegate() {
                return delegate;
            }
        };
    }

    /**
     * Ensures that given URI will contain proper 'role' query parameter as
     * required by {@link TransportFactory}'s method
     * 
     * @param inUri URI to check(and eventually fix) for presence of 'role'
     *            parameter
     * @param role {@link EndpointRole} to use
     * @return URI which will contain 'role' query parameter as requested
     * @throws URISyntaxException
     */
    public static String ensureRole(String inUri, EndpointRole role) throws URISyntaxException {
        int idx = inUri.indexOf('?');
        if (idx == -1) {
            return inUri + "?" + "role=" + role.name();
        } else {
            return inUri.substring(0, idx + 1) + org.opendaylight.jsonrpc.bus.messagelib.Util
                    .replaceParam(inUri.substring(idx + 1), "role", role.name());
        }
    }

    /**
     * Populates {@link HierarchicalEnumMap} entries from list of endpoints
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
     * callback
     *
     * @param closeable {@link AutoCloseable} instance to close, can be null
     * @param callback Callback to be invoked when exception occur, must not be
     *            null
     */
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
     * Create a Binding Independent path identifier for a name
     */
    public static YangInstanceIdentifier createBiPath(final String name) {
        final YangInstanceIdentifier.InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        builder.node(Config.QNAME).node(ConfiguredEndpoints.QNAME).nodeWithKey(ConfiguredEndpoints.QNAME,
                QName.create(ConfiguredEndpoints.QNAME.getNamespace(), ConfiguredEndpoints.QNAME.getRevision(), "name"),
                name);
        return builder.build();
    }
}
