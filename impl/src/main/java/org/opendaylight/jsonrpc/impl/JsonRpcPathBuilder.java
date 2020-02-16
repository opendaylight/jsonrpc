/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;

import org.opendaylight.yangtools.concepts.Builder;

/**
 * Fluent {@link Builder} of JSONRPC path.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 16, 2020
 */
public final class JsonRpcPathBuilder implements Builder<JsonObject> {
    private static class PathArgument {
        private final Optional<String> name;
        private final List<Entry<String, String>> keys = new ArrayList<>(2);

        PathArgument(String name) {
            this.name = Optional.of(name);
            this.keys.addAll(Collections.emptyList());
        }

        PathArgument(Collection<Entry<String, String>> keys) {
            this.name = Optional.empty();
            this.keys.addAll(keys);
        }

        static PathArgument of(String item) {
            return new PathArgument(item);
        }

        static PathArgument of(Collection<Entry<String, String>> keys) {
            return new PathArgument(keys);
        }

        boolean isKeyed() {
            return !keys.isEmpty();
        }
    }

    private final Deque<PathArgument> path = new LinkedList<>();

    private JsonRpcPathBuilder() {
        // NOOP
    }

    public static JsonRpcPathBuilder newBuilder(String container) {
        return new JsonRpcPathBuilder().container(container);
    }

    public static JsonRpcPathBuilder newBuilder() {
        return new JsonRpcPathBuilder();
    }

    @Override
    public JsonObject build() {
        JsonElement parent = new JsonObject();
        final JsonObject root = parent.getAsJsonObject();
        for (;;) {
            final PathArgument current = path.poll();
            if (current == null) {
                break;
            }
            if (current.isKeyed()) {
                final JsonObject next = new JsonObject();
                for (Entry<String, String> entry : current.keys) {
                    next.addProperty(entry.getKey(), entry.getValue());
                }
                parent.getAsJsonArray().add(next);
                parent = next;
            } else {
                final JsonElement next = isNextKeyed(path) ? new JsonArray() : new JsonObject();
                parent.getAsJsonObject().add(current.name.get(), next);
                parent = next;
            }
        }
        return root;
    }

    private static boolean isNextKeyed(Deque<PathArgument> stack) {
        final PathArgument element = stack.peek();
        return element != null && element.isKeyed();
    }

    /**
     * Append container (or list) to path arguments.
     *
     * @param container name of element to append
     * @return this builder instance
     */
    public JsonRpcPathBuilder container(@NonNull String container) {
        path.add(PathArgument.of(container));
        return this;
    }

    /**
     * Append individual list item into path arguments.
     *
     * @param key list item key name
     * @param value list item key value
     * @return this builder instance
     */
    public JsonRpcPathBuilder item(@NonNull String key, @NonNull String value) {
        item(Collections.singletonList(new AbstractMap.SimpleEntry<>(key, value)));
        return this;
    }

    /**
     * Append individual list item into path arguments.
     *
     * @param key1 list item key1 name
     * @param value1 list item key1 value
     * @param key2 list item key2 name
     * @param value2 list item key2 value
     * @return this builder instance
     */
    public JsonRpcPathBuilder item(@NonNull String key1, @NonNull String value1, @NonNull String key2,
            @NonNull String value2) {
        item(ImmutableList.of(new AbstractMap.SimpleEntry<>(key1, value1),
                new AbstractMap.SimpleEntry<>(key2, value2)));
        return this;
    }

    /**
     * Append individual list item into path arguments.
     *
     * @param keys {@link Collection} of item keys (name/value pairs)
     * @return this builder instance
     */
    public JsonRpcPathBuilder item(@NonNull Collection<Entry<String, String>> keys) {
        Objects.requireNonNull(keys);
        path.add(PathArgument.of(keys));
        return this;
    }
}
