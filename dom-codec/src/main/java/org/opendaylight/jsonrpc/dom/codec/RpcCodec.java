/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.function.Supplier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.ContainerLike;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RpcCodec extends RpcNotificationBaseCodec<ContainerNode> {
    private static final Logger LOG = LoggerFactory.getLogger(RpcCodec.class);

    private final String type;

    static RpcCodec create(EffectiveModelContext context, RpcDefinition definition, String type,
            Supplier<ContainerLike> schemaSupplier) {
        final ContainerLike schema = schemaSupplier.get();
        final Absolute path = Absolute.of(definition.getQName(), schema.getQName());
        return new RpcCodec(context, path, definition, type, schema);
    }

    RpcCodec(EffectiveModelContext context, Absolute path, RpcDefinition definition, String type,
            DataNodeContainer schema) {
        super(context, definition.getQName().getLocalName(), path, schema.getChildNodes().isEmpty(), schema);
        this.type = type;
    }

    @Override
    public ContainerNode deserialize(JsonElement input) throws IOException {
        LOG.trace("[decode][{}][{}] input : {}", shortName, type, input);
        final ContainerNode result;
        if (isEmpty || input == null || input.isJsonNull()) {
            result = ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(new NodeIdentifier(path.lastNodeIdentifier()))
                .build();
        } else {
            result = decode(wrapInputIfNecessary(input));
        }
        LOG.trace("[decode][{}][{}] result : {}", shortName, type, result);
        return result;
    }

    private ContainerNode decode(JsonElement input) throws IOException {
        final var builder = createNodeBuilder(path.lastNodeIdentifier());
        try (NormalizedNodeStreamWriter writer = createWriter(builder);
                JsonParserStream jsonParser = JsonParserStream.create(writer, jsonCodec(),
                    SchemaInferenceStack.of(context, path).toInference())) {
            jsonParser.parse(JsonReaderAdapter.from(input));
            return builder.build();
        }
    }

    @Override
    public JsonElement serialize(ContainerNode input) throws IOException {
        LOG.trace("[encode][{}][{}] input {} ", shortName, type, input);
        final JsonObject result = encode(input);
        LOG.trace("[encode][{}][{}] result : {}", shortName, type, result);
        return result;
    }
}
