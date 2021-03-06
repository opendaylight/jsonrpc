/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.dom.codec.CodecUtils;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class containing common fields for all JSONRPC components (Data
 * broker, Notifications, RPCs).
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 21, 2018
 */
abstract class AbstractJsonRPCComponent {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractJsonRPCComponent.class);
    protected final EffectiveModelContext schemaContext;
    protected final JsonRpcCodecFactory codecFactory;
    protected final TransportFactory transportFactory;
    protected final HierarchicalEnumMap<JsonElement, DataType, String> pathMap;
    protected final Peer peer;

    AbstractJsonRPCComponent(@NonNull final EffectiveModelContext schemaContext,
            @NonNull TransportFactory transportFactory,
            @NonNull HierarchicalEnumMap<JsonElement, DataType, String> pathMap,
            @NonNull JsonRpcCodecFactory codecFactory, @NonNull Peer peer) {
        this.schemaContext = Objects.requireNonNull(schemaContext);
        this.transportFactory = Objects.requireNonNull(transportFactory);
        this.pathMap = Objects.requireNonNull(pathMap);
        this.peer = Objects.requireNonNull(peer);
        this.codecFactory = Objects.requireNonNull(codecFactory);
    }

    protected JsonObject createRootPath(Module module, QName node) {
        final String topLevel = CodecUtils.makeQualifiedName(module, node);
        final JsonObject path = new JsonObject();
        path.add(topLevel, new JsonObject());
        return path;
    }

    protected String getEndpoint(final DataType type, final RemoteGovernance governance, final JsonObject path) {
        String endpoint = pathMap.lookup(path, type).orElse(null);
        LOG.debug("[{}][{}] map lookup => {}", peer.getName(), type, endpoint);
        if (endpoint == null && governance != null) {
            endpoint = governance.governance(-1, peer.getName(), path);
            LOG.debug("[{}][{}] governance lookup => {}", peer.getName(), type, endpoint);
            if (endpoint != null) {
                pathMap.put(path, type, endpoint);
            }
        }
        return endpoint;
    }
}
