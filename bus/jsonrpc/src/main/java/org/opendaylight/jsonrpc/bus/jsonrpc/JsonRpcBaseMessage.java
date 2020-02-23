/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.Type;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for JSON RPC messages. The derived classes are used for
 * the actual JSON RPC messages. These classes can be used for both sending and
 * receiving messages. When used for sending messages, a custom serializer
 * should be written for Object to JSON conversion.
 *
 * @author Shaleen Saxena
 */
public abstract class JsonRpcBaseMessage {
    public enum JsonRpcMessageType {
        REQUEST,      // A request which expects a reply
        NOTIFICATION, // A notification which has no reply
        REPLY,        // Reply with either a result or error in it
        PARSE_ERROR   // Incoming invalid message is marshaled into this pseudo-message
    }

    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcBaseMessage.class);
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    protected static final String VERSION = "2.0";
    protected static final String VERSION_SHORT = "2";

    private final String jsonrpc;
    private final JsonElement id;
    private final JsonObject metadata;

    protected JsonRpcBaseMessage(AbstractBuilder<?, ?> builder) {
        this.jsonrpc = Objects.requireNonNull(builder.jsonrpc);
        this.id = builder.id;
        this.metadata = builder.metadata;
    }

    @NonNull
    public String getJsonrpc() {
        return jsonrpc;
    }

    @Nullable
    public JsonObject getMetadata() {
        return metadata;
    }

    @Nullable
    public JsonElement getId() {
        return id;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public int getIdAsIntValue() {
        try {
            return getId().getAsJsonPrimitive().getAsNumber().intValue();
        } catch (RuntimeException e) {
            LOG.debug("Unable to parse ID as int", e);
        }
        return 0;
    }

    /*
     * Convenience function for converting Gson's JsonElement to an object. The
     * conversion is achieved by converting JsonElement to JSON and then parsed
     * as the requested object. Perhaps not very efficient, but very convenient
     * though.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    protected static <T> T convertJsonElementToClass(JsonElement elem,
            Type type) throws JsonRpcException {
        try {
            return GSON.fromJson(GSON.toJson(elem), type);
        } catch (RuntimeException e) {
            throw new JsonRpcException(e);
        }
    }

    /*
     * Convenience function for converting an Object to Gson's JsonElement. The
     * conversion is achieved by converting Object to JSON and then parsed back
     * as JsonElement. Perhaps not very efficient, but very convenient though.
     */
    protected static JsonElement convertClassToJsonElement(Object obj) {
        if (JsonElement.class.isInstance(obj)) {
            return (JsonElement) obj;
        } else {
            return GSON.fromJson(GSON.toJson(obj), JsonElement.class);
        }
    }

    public static String getSupportedVersion() {
        return VERSION;
    }

    public static boolean isSupportedVersion(String version) {
        return VERSION.equals(version) || VERSION_SHORT.equals(version);
    }

    @NonNull
    public abstract JsonRpcMessageType getType();

    public abstract static class AbstractBuilder<T extends AbstractBuilder<T, M>, M extends JsonRpcBaseMessage> {
        private String jsonrpc;
        private JsonElement id;
        private JsonObject metadata;

        protected AbstractBuilder() {
            jsonrpc = VERSION;
        }

        protected AbstractBuilder(M copyFrom) {
            this.id = copyFrom.getId();
            this.jsonrpc = copyFrom.getJsonrpc();
            this.metadata = copyFrom.getMetadata();
        }

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        public T jsonrpc(String value) {
            this.jsonrpc = value;
            return self();
        }

        public T id(JsonElement value) {
            this.id = value;
            return self();
        }

        public T idFromIntValue(int value) {
            return id(new JsonPrimitive(value));
        }

        public T metadata(JsonObject value) {
            this.metadata = value;
            return self();
        }

        public final M build() {
            return newInstance();
        }

        protected abstract M newInstance();
    }
}
