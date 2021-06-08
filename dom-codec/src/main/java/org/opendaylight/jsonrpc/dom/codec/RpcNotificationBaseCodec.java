/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import static org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter.forStreamWriter;
import static org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter.createNestedWriter;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

/**
 * Base class that contain common code to deal with RPCs and notifications.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Oct 18, 2020
 */
abstract class RpcNotificationBaseCodec<T> extends AbstractCodec implements Codec<JsonElement, T, IOException> {
    protected final String shortName;
    protected final Absolute path;
    protected final boolean isEmpty;
    protected final DataNodeContainer schema;

    RpcNotificationBaseCodec(EffectiveModelContext context, String shortName, Absolute path, boolean isEmpty,
            DataNodeContainer schema) {
        super(context);
        this.shortName = shortName;
        this.path = path;
        this.isEmpty = isEmpty;
        this.schema = schema;
    }

    protected JsonObject wrapInputIfNecessary(JsonElement input) {
        if (input.isJsonPrimitive()) {
            return wrapPrimitive(input.getAsJsonPrimitive());
        } else if (input.isJsonArray()) {
            return wrapArray(input.getAsJsonArray());
        } else {
            return input.getAsJsonObject();
        }
    }

    protected JsonObject wrapPrimitive(JsonPrimitive input) {
        final JsonObject result = new JsonObject();
        result.add(Iterables.getOnlyElement(schema.getChildNodes()).getQName().getLocalName(), input);
        return result;
    }

    protected JsonObject wrapArray(JsonArray input) {
        Preconditions.checkArgument(input.size() == schema.getChildNodes().size(),
                "Number of input array elements (%s) does not match number of child schema nodes (%s)", input.size(),
                schema.getChildNodes().size());
        final JsonObject result = new JsonObject();
        final List<? extends DataSchemaNode> items = new ArrayList<>(schema.getChildNodes());
        for (int i = 0; i < items.size(); i++) {
            result.add(items.get(i).getQName().getLocalName(), input.get(i));
        }
        return result;
    }

    protected JsonObject encode(ContainerNode input) throws IOException {
        final StringWriter writer = new StringWriter();
        final JsonWriter jsonWriter = JsonWriterFactory.createJsonWriter(writer);
        try (NormalizedNodeStreamWriter streamWriter = createNestedWriter(jsonCodec(), path, null, jsonWriter);
                NormalizedNodeWriter nodeWriter = forStreamWriter(streamWriter)) {
            jsonWriter.beginObject();
            for (final DataContainerChild child : input.body()) {
                nodeWriter.write(child);
            }
            jsonWriter.endObject();
            jsonWriter.flush();
        }
        final JsonObject encoded = parseFromWriter(writer);
        final JsonObject result = new JsonObject();
        for (final Entry<String, JsonElement> element : encoded.entrySet()) {
            final String property = element.getKey();
            final int idx = element.getKey().indexOf(':');
            if (idx != -1) {
                result.add(property.substring(idx + 1), element.getValue());
            } else {
                result.add(property, element.getValue());
            }
        }
        return result;
    }
}
