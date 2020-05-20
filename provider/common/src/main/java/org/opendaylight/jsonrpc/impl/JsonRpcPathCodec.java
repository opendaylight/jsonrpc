/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Codec} to perform transformation between {@link YangInstanceIdentifier} and JSONRPC path. Instance of this
 * codec is safe to use from multiple threads concurrently.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 15, 2020
 */
public final class JsonRpcPathCodec implements Codec<JsonObject, YangInstanceIdentifier, RuntimeException> {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcPathCodec.class);
    private final SchemaContext schemaContext;

    private JsonRpcPathCodec(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    /**
     * Create new instance of this codec for given {@link SchemaContext}.
     *
     * @param schemaContext {@link SchemaContext} to use
     * @return new instance of {@link JsonRpcPathCodec}
     */
    public static JsonRpcPathCodec create(@NonNull SchemaContext schemaContext) {
        return new JsonRpcPathCodec(Objects.requireNonNull(schemaContext));
    }

    @Override
    public YangInstanceIdentifier deserialize(@NonNull JsonObject input) {
        final InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        decodeObject(builder, Objects.requireNonNull(input), null, null);
        return builder.build();
    }

    @Override
    public JsonObject serialize(@NonNull YangInstanceIdentifier input) {
        Objects.requireNonNull(input);
        final Iterator<PathArgument> it = input.getPathArguments().iterator();
        final JsonRpcPathBuilder builder = JsonRpcPathBuilder.newBuilder();
        String module = null;
        while (it.hasNext()) {
            final PathArgument current = it.next();
            if (current instanceof YangInstanceIdentifier.AugmentationIdentifier) {
                continue;
            }
            final String currentModule = ensureModuleFound(schemaContext.findModule(current.getNodeType().getModule()),
                    current.getNodeType().toString()).getName();
            if (current instanceof NodeIdentifierWithPredicates) {
                final Set<Entry<QName, Object>> inKeys = ((NodeIdentifierWithPredicates) current).entrySet();
                final Collection<Entry<String, String>> keys = inKeys.stream()
                        .map(JsonRpcPathCodec::mapKey)
                        .collect(Collectors.toList());
                LOG.trace("[Encode][Keyed  ]: {}, {}", current, keys);
                builder.item(keys);
            } else {
                LOG.trace("[Encode][Unkeyed]: {}", current);
                builder.container(prefixElement(current.getNodeType(), currentModule, module));
            }
            module = currentModule;
        }
        return builder.build();
    }

    private String prefixElement(QName name, String currentModule, String previous) {
        if (previous == null || !currentModule.equals(previous)) {
            return currentModule + ':' + name.getLocalName();
        }
        return name.getLocalName();
    }

    private static Entry<String, String> mapKey(Entry<QName, Object> entry) {
        return new AbstractMap.SimpleEntry<>(entry.getKey().getLocalName(), String.valueOf(entry.getValue()));
    }

    private YangInstanceIdentifier decodeObject(InstanceIdentifierBuilder builder, JsonObject path, QName nodeNs,
            QName localNs) {
        LOG.trace("[Decode][Object]: {}", path);
        final Iterator<Entry<String, JsonElement>> it = path.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, JsonElement> entry = it.next();
            final String key = entry.getKey();
            final JsonElement value = entry.getValue();
            if (key.indexOf(':') == -1) {
                localNs = QName.create(nodeNs, key);
            } else {
                final String[] parts = key.split(":");
                nodeNs = constructModuleQName(parts[0]);
                if (value instanceof JsonArray || value instanceof JsonObject) {
                    localNs = QName.create(nodeNs, parts[1]);
                    nodeNs = localNs;
                }
            }
            if (value instanceof JsonObject) {
                builder.node(localNs);
                decodeObject(builder, value.getAsJsonObject(), nodeNs, localNs);
            } else if (value instanceof JsonArray) {
                builder.node(localNs);
                nodeNs = localNs;
                decodeArray(builder, value.getAsJsonArray(), nodeNs, localNs);
            } else if (value instanceof JsonPrimitive) {
                decodeLeaf(builder, nodeNs, localNs, entry.getValue().getAsJsonPrimitive());
            } else {
                throw new IllegalStateException(
                        String.format("Unexpected element : %s => %s", value.getClass().getSimpleName(), value));
            }
        }
        return builder.build();
    }

    private void decodeLeaf(InstanceIdentifierBuilder builder, QName nodeNs, QName localNs, JsonPrimitive leaf) {
        LOG.trace("[Decode][Leaf  ]: {}", leaf);
        builder.nodeWithKey(nodeNs, localNs, leaf.getAsString());
    }

    private void decodeArray(InstanceIdentifierBuilder builder, JsonArray array, QName nodeNs, QName localNs) {
        LOG.trace("[Decode][Array ]: {}", array);
        for (final JsonElement je : array) {
            if (je instanceof JsonObject) {
                decodeObject(builder, je.getAsJsonObject(), nodeNs, localNs);
            } else {
                throwJsonPathError(je);
            }
        }
    }

    private Module ensureModuleFound(Optional<? extends Module> module, String name) {
        return module.orElseThrow(
            () -> new IllegalArgumentException(String.format("Module '%s' not found in schema", name)));
    }

    private Module findModule(String moduleName) {
        return ensureModuleFound(schemaContext.findModules(moduleName).stream().findFirst(), moduleName);
    }

    private QName constructModuleQName(String localName) {
        return QName.create(findModule(localName).getQNameModule(), localName);
    }

    private void throwJsonPathError(JsonElement ex) {
        throw new IllegalStateException(
                String.format("Unexpected JsonElement : %s => %s", ex.getClass().getSimpleName(), ex));
    }
}
