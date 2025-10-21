/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.StringWriter;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.builder.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;

abstract class AbstractCodec {
    protected static final String COLON = ":";
    protected final EffectiveModelContext context;

    AbstractCodec(final @NonNull EffectiveModelContext context) {
        this.context = Objects.requireNonNull(context);
    }

    protected static DataContainerNodeBuilder<NodeIdentifier, ContainerNode> createNodeBuilder(QName qname) {
        return ImmutableNodes.newContainerBuilder().withNodeIdentifier(NodeIdentifier.create(qname));
    }

    protected static NormalizedNodeStreamWriter createWriter(NormalizedNodeContainerBuilder<?, ?, ?, ?> builder) {
        return ImmutableNormalizedNodeStreamWriter.from(builder);
    }

    /**
     * Get {@link Module} from from {@link EffectiveModelContext} based on {@link QNameModule}.
     *
     * @param context {@link EffectiveModelContext}
     * @param nameModule {@link QNameModule} to fine in {@link EffectiveModelContext}
     * @return resolved {@link Module}
     * @throws IllegalStateException If module can't be found exception
     */
    protected static Module getModule(EffectiveModelContext context, QNameModule nameModule) {
        return context.findModule(nameModule).orElseThrow(
            () -> new IllegalStateException("Could not find module for namespace %s and revision %s".formatted(
                nameModule.namespace(), nameModule.revision())));
    }

    /**
     * Create qualified name for local node.
     *
     * @param module {@link Module} used to get element prefix
     * @param qname {@link QName} used to obtain local element name
     * @return qualified name
     */
    protected static String makeQualifiedName(Module module, QName qname) {
        return new StringBuilder().append(module.getName()).append(COLON).append(qname.getLocalName()).toString();
    }

    protected static JsonObject wrap(JsonElement input, String prefix) {
        final JsonObject result = new JsonObject();
        result.add(prefix, input);
        return result;
    }

    protected static JsonObject wrapInArray(JsonElement input, String prefix) {
        final JsonArray arr = new JsonArray();
        arr.add(input);
        return wrap(arr, prefix);
    }

    /**
     * Feed content of given {@link StringWriter} into {@link JsonParser} and return {@link JsonObject} out of it.
     *
     * @param writer {@link StringWriter} to read JSON string from
     * @return parsed {@link JsonObject}
     */
    protected static JsonObject parseFromWriter(StringWriter writer) {
        return JsonParser.parseString(writer.toString()).getAsJsonObject();
    }

    protected JSONCodecFactory jsonCodec() {
        return JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.getShared(context);
    }
}
