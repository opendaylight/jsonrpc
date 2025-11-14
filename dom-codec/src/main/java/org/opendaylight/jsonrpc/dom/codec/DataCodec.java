/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DataCodec extends AbstractCodec implements Codec<JsonElement, NormalizedNode, IOException> {
    private static final Logger LOG = LoggerFactory.getLogger(DataCodec.class);
    private final YangInstanceIdentifier path;
    private final Inference parentSchemaNode;

    DataCodec(@NonNull EffectiveModelContext context, @NonNull YangInstanceIdentifier path) {
        super(context);
        this.path = Objects.requireNonNull(path);
        final SchemaInferenceStack stack = DataSchemaContextTree.from(context)
                .enterPath(path)
                .orElseThrow()
                .stack();
        if (!stack.isEmpty()) {
            stack.exit();
        }
        parentSchemaNode = stack.toInference();
    }

    private static class DecoderPathWalker extends PathWalker<JsonObject> {
        private String prefix;
        private QName nodeType;
        private Module module;
        private PathArgument last;
        private final JsonElement input;
        private final EffectiveModelContext context;

        DecoderPathWalker(EffectiveModelContext context, YangInstanceIdentifier path, JsonElement input) {
            super(path);
            this.input = input;
            this.context = context;
        }

        @Override
        protected void visitPathArgument(PathArgument arg) {
            nodeType = arg.getNodeType();
            module = getModule(context, nodeType.getModule());
            prefix = makeQualifiedName(module, arg.getNodeType());
            last = arg;
        }

        @Override
        protected JsonObject result() {
            if (last instanceof NodeIdentifierWithPredicates) {
                return wrapInArray(input, prefix);
            }
            return wrap(input, prefix);
        }
    }

    @Override
    public NormalizedNode deserialize(JsonElement input) throws IOException {
        LOG.trace("[Decode] input : {}", input);
        if (input == null || input.isJsonNull()) {
            return returnResult(null);
        }
        return returnResult(deserializeWrapped(new DecoderPathWalker(context, path, input).walk()));
    }

    private static NormalizedNode returnResult(NormalizedNode result) {
        LOG.trace("[Decode] result : {}", result);
        return result;
    }

    private NormalizedNode deserializeWrapped(JsonObject input) throws IOException {
        final NormalizationResultHolder resultHolder = new NormalizationResultHolder();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
        try (JsonParserStream jsonParser = JsonParserStream.create(writer, jsonCodec(), parentSchemaNode)) {
            jsonParser.parse(JsonReaderAdapter.from(input));
        }
        NormalizedNode result = resultHolder.getResult().data();
        if (result instanceof MapNode && !((MapNode) result).isEmpty()) {
            result = Iterables.getOnlyElement(((MapNode) result).body());
        }
        return result;
    }

    private static class EncoderPathWalker extends PathWalker<String> {
        private QName last = null;
        private final EffectiveModelContext context;

        EncoderPathWalker(EffectiveModelContext context, YangInstanceIdentifier path) {
            super(path);
            this.context = context;
        }

        @Override
        protected void visitPathArgument(PathArgument arg) {
            if (arg instanceof NodeIdentifierWithPredicates) {
                return;
            }
            last = arg.getNodeType();
        }

        @Override
        protected String result() {
            Objects.requireNonNull(last, "Path does not contain any usable QName");
            return getModule(context, last.getModule()).getName() + COLON + last.getLocalName();
        }
    }

    @Override
    public JsonElement serialize(NormalizedNode input) throws IOException {
        LOG.trace("[Decode] input : {}", input);
        if (input == null) {
            return null;
        }
        final String outer = new EncoderPathWalker(context, path).walk();
        final JsonElement dataJson = unwrap(encode(input), outer);
        LOG.trace("[Decode] result : {}", dataJson);
        return dataJson;
    }

    /**
     * Unwrap outer {@link JsonElement} and make sure returned {@link JsonElement} is object. Input could possibly be
     * JsonArray with single item if encoding list item.
     *
     * @param input encoded JSON data
     * @param outerElement name of outer element composed from module name and local name
     * @return unwrapped {@link JsonObject}
     */
    private static JsonElement unwrap(JsonObject input, String outerElement) {
        final JsonElement el = input.get(outerElement);
        if (el.isJsonArray()) {
            return el.getAsJsonArray().get(0).getAsJsonObject();
        } else {
            return el;
        }
    }

    /**
     * Performs the actual data conversion.
     *
     * @param data {@link NormalizedNode} to encode into {@link JsonObject}
     * @return data converted as a JsonObject
     * @throws IOException if underlying JSON codec raises error
     */
    private JsonObject encode(NormalizedNode data) throws IOException {
        try (StringWriter writer = new StringWriter();
                JsonWriter jsonWriter = JsonWriterFactory.createJsonWriter(writer)) {
            final JSONCodecFactory codecFactory = jsonCodec();
            final NormalizedNodeStreamWriter jsonStream = JSONNormalizedNodeStreamWriter
                    .createNestedWriter(codecFactory, parentSchemaNode, null, jsonWriter);
            try (NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(jsonStream)) {
                jsonWriter.beginObject();
                if (data instanceof MapEntryNode mapEntry) {
                    nodeWriter.write(ImmutableNodes.newSystemMapBuilder()
                        .withNodeIdentifier(new NodeIdentifier(mapEntry.name().getNodeType()))
                        .withChild(mapEntry)
                        .build());
                } else {
                    nodeWriter.write(data);
                }
                jsonWriter.endObject();
                nodeWriter.flush();
            }
            return parseFromWriter(writer);
        }
    }
}
