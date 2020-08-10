/*
 * Copyright (c) 2020 dNation.tech. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.mdsal.dom.api.DOMEvent;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.util.ContainerSchemaNodes;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NotificationCodec extends RpcNotificationBaseCodec<DOMNotification> {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationCodec.class);
    private final NotificationDefinition definition;

    static NotificationCodec create(EffectiveModelContext context, NotificationDefinition definition) {
        final Absolute path = Absolute.of(definition.getQName());
        return new NotificationCodec(context, path, definition);
    }

    NotificationCodec(@NonNull EffectiveModelContext context, Absolute path, NotificationDefinition definition) {
        super(context, definition.getQName().getLocalName(), path, definition.getChildNodes().isEmpty(), definition);
        this.definition = Objects.requireNonNull(definition);
    }

    @Override
    public DOMNotification deserialize(JsonElement input) throws IOException {
        LOG.trace("[decode][{}] input : {}", shortName, input);
        final DOMNotification result;
        if (isEmpty || input == null || input.isJsonNull()) {
            result = new JsonRpcNotification(path);
        } else {
            final JsonObject fixed = wrapInputIfNecessary(input);
            final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> builder = createNodeBuilder(
                    path.lastNodeIdentifier());
            try (NormalizedNodeStreamWriter streamWriter = createWriter(builder);
                    JsonParserStream jsonParser = JsonParserStream.create(streamWriter, jsonCodec(),
                            ContainerSchemaNodes.forNotification(definition))) {
                jsonParser.parse(JsonReaderAdapter.from(fixed));
                result = new JsonRpcNotification(builder.build(), path);
            }
        }
        LOG.trace("[decode][{}] result : {}", shortName, result);
        return result;
    }

    @Override
    public JsonElement serialize(DOMNotification input) throws IOException {
        LOG.trace("[encode][{}] input {} ", shortName, input);
        final JsonObject result = encode(input.getBody());
        LOG.trace("[encode][{}] result : {}", shortName, result);
        return result;
    }

    @NonNullByDefault
    private static final class JsonRpcNotification implements DOMNotification, DOMEvent {
        private final ContainerNode content;
        private final Absolute schemaPath;
        private final Instant eventTime;

        JsonRpcNotification(final ContainerNode content, final Instant eventTime, final Absolute schemaPath) {
            this.content = content;
            this.eventTime = eventTime;
            this.schemaPath = schemaPath;
        }

        JsonRpcNotification(final Absolute schemaPath) {
            this(ImmutableContainerNodeBuilder.create().build(), schemaPath);
        }

        JsonRpcNotification(final ContainerNode content, final Absolute schemaPath) {
            this(content, Instant.now(), schemaPath);
        }

        @Override
        public @NonNull Absolute getType() {
            return schemaPath;
        }

        @Nullable
        @Override
        public ContainerNode getBody() {
            return content;
        }

        @Override
        public Instant getEventInstant() {
            return eventTime;
        }

        @Override
        public String toString() {
            return "JsonRpcNotification [eventTime=" + eventTime + ", content=" + content + ", schemaPath=" + schemaPath
                    + "]";
        }
    }
}
