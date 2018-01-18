/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses {@link YangInstanceIdentifier} from JSON-RPC 2.0 path specification,
 * using provided {@link SchemaContext}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class YangInstanceIdentifierDeserializer {
    private static final Logger LOG = LoggerFactory.getLogger(YangInstanceIdentifierDeserializer.class);

    private YangInstanceIdentifierDeserializer() {
        // not allowed to instantiate directly
    }

    public static YangInstanceIdentifier toYangInstanceIdentifier(JsonElement path, SchemaContext schemaContext) {
        final ParsingContext pc = new ParsingContext(schemaContext);
        return pc.parse(path);
    }

    private static class ParsingContext {
        private final SchemaContext schemaContext;
        private final InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        private QName nodeNs;
        private QName localNs;

        private ParsingContext(final SchemaContext schemaContext) {
            this.schemaContext = schemaContext;
        }

        private static void assertNamespace(QName ns) {
            Preconditions.checkNotNull(ns, "Missing/unresolvable namespace");
        }

        private void throwJsonPathError(JsonElement e) {
            throw new IllegalStateException(
                    String.format("Unexpected JsonElement : %s => %s", e.getClass().getSimpleName(), e));
        }

        private QName lookupByLocalName(String localName) {
            return Util.findModuleWithLatestRevision(schemaContext, localName)
                .map(module -> QName.create(module.getQNameModule(), localName)).orElse(null);
        }

        private YangInstanceIdentifier parse(JsonElement path) {
            Preconditions.checkArgument(path instanceof JsonObject,
                    "Root element must be instance of JsonObject, actual type is %s", path.getClass().getSimpleName());
            processObject(path);
            return builder.build();
        }

        private void processArray(JsonElement path) {
            LOG.debug("Current node [ARRAY ]: {}", path);
            final JsonArray arr = (JsonArray) path;
            for (final JsonElement je : arr) {
                if (je instanceof JsonObject) {
                    processObject(je);
                    continue;
                } else {
                    throwJsonPathError(je);
                }
            }
        }

        private YangInstanceIdentifier processObject(JsonElement path) {
            LOG.debug("Current node [OBJECT]: {}", path);
            final Iterator<Entry<String, JsonElement>> it = path.getAsJsonObject().entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, JsonElement> e = it.next();
                final String currentNode = e.getKey();
                final JsonElement el = e.getValue();
                // No namespace prefix
                if (currentNode.indexOf(':') == -1) {
                    localNs = QName.create(nodeNs, currentNode);
                    assertNamespace(localNs);
                } else {
                    final String[] localParts = currentNode.split(":");
                    nodeNs = lookupByLocalName(localParts[0]);
                    assertNamespace(nodeNs);
                    updateNodeNamespace(el, localParts[1]);
                }
                if (el instanceof JsonObject) {
                    builder.node(localNs);
                    processObject(e.getValue());
                    continue;
                }
                if (el instanceof JsonArray) {
                    builder.node(localNs);
                    nodeNs = localNs;
                    processArray(e.getValue());
                    continue;
                }
                if (el instanceof JsonPrimitive) {
                    processLeaf(e.getValue());
                    continue;
                }
                throwJsonPathError(e.getValue());
            }
            return builder.build();
        }

        private void updateNodeNamespace(JsonElement el, String namespace) {
            if (el instanceof JsonArray || el instanceof JsonObject) {
                localNs = QName.create(nodeNs, namespace);
                nodeNs = localNs;
            }
        }

        private void processLeaf(final JsonElement path) {
            LOG.debug("Current node [LEAF  ]: {}", path);
            builder.nodeWithKey(nodeNs, localNs, path.getAsJsonPrimitive().getAsString());
        }
    }
}
