/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.gson.JsonElement;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Abstract class containing common fields for all JSONRPC components (Data
 * broker, Notifications, RPCs).
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 21, 2018
 */
abstract class AbstractJsonRPCComponent {
    protected final SchemaContext schemaContext;
    protected final JsonConverter jsonConverter;
    protected final TransportFactory transportFactory;
    protected final HierarchicalEnumMap<JsonElement, DataType, String> pathMap;
    protected final Peer peer;

    AbstractJsonRPCComponent(@NonNull final SchemaContext schemaContext, @NonNull TransportFactory transportFactory,
            @NonNull HierarchicalEnumMap<JsonElement, DataType, String> pathMap, @NonNull JsonConverter jsonConverter,
            @NonNull Peer peer) {
        this.schemaContext = Objects.requireNonNull(schemaContext);
        this.transportFactory = Objects.requireNonNull(transportFactory);
        this.pathMap = Objects.requireNonNull(pathMap);
        this.jsonConverter = Objects.requireNonNull(jsonConverter);
        this.peer = Objects.requireNonNull(peer);
    }
}
