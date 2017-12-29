/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.hmap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link PathCodec} which consumes {@link JsonElement} and
 * produce sequence of {@link String} node identifiers.
 *
 * @see PathCodec
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public final class JsonPathCodec implements PathCodec<JsonElement, String> {
    private static final Logger LOG = LoggerFactory.getLogger(JsonPathCodec.class);
    private static final JsonPathCodec INSTANCE = new JsonPathCodec();

    private JsonPathCodec() {
        // no public constructor, use factory method
    }

    public static JsonPathCodec create() {
        return INSTANCE;
    }

    @Override
    public Iterable<String> serialize(JsonElement path) {
        final LinkedList<String> list = new LinkedList<>();
        serializeObject(path, list);
        return list;
    }

    private void serializeObject(JsonElement path, LinkedList<String> list) {
        LOG.trace("Current node [OBJECT]: {}", path);
        final Iterator<Entry<String, JsonElement>> it = path.getAsJsonObject().entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, JsonElement> e = it.next();
            final String currentNode = e.getKey();
            final JsonElement el = e.getValue();
            if (el instanceof JsonObject) {
                list.addLast(currentNode);
                serializeObject(e.getValue(), list);
            } else if (el instanceof JsonArray) {
                list.addLast(currentNode);
                serializeArray(e.getValue(), list);
            } else {
                serializeLeaf(currentNode, e.getValue(), list);
            }
        }
    }

    private void serializeLeaf(String currentNode, JsonElement path, LinkedList<String> list) {
        LOG.trace("Current node [LEAF  ]: {}", path);
        list.addLast(currentNode + "=" + path.getAsJsonPrimitive().getAsString());
    }

    private void serializeArray(JsonElement path, LinkedList<String> list) {
        LOG.trace("Current node [ARRAY ]: {}", path);
        final JsonArray arr = (JsonArray) path;
        for (final JsonElement je : arr) {
            if (je instanceof JsonObject) {
                serializeObject(je, list);
                continue;
            } else {
                throwJsonPathError(je);
            }
        }
    }

    private void throwJsonPathError(JsonElement je) {
        throw new IllegalStateException("Unexpected JSON element " + je);
    }

    @Override
    @SuppressWarnings("squid:S2259")
    public JsonElement deserialize(Iterable<String> path) {
        final JsonObject root = new JsonObject();
        JsonObject current = root;
        JsonObject prev = null;
        String last = null;
        final Iterator<String> it = path.iterator();
        if (it.hasNext()) {
            it.next(); // skip root
        }
        while (it.hasNext()) {
            final String p = it.next();
            final int eqIdx = p.indexOf('=');
            if (eqIdx != -1) {
                final String part1 = p.substring(0, eqIdx);
                final String part2 = p.substring(eqIdx + 1);
                final JsonArray arr = new JsonArray();
                final JsonObject el = new JsonObject();
                el.add(part1, new JsonPrimitive(part2));
                arr.add(el);
                prev.add(last, arr);
                current = el;
            } else {
                final JsonObject node = new JsonObject();
                prev = current;
                current.add(p, node);
                current = node;
                last = p;
            }
        }
        return root;
    }
}
