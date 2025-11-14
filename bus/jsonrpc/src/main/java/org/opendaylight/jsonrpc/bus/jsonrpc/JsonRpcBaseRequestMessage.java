/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Base for a request message.
 *
 * @author Thomas Pantelis
 */
public abstract class JsonRpcBaseRequestMessage extends JsonRpcBaseMessage {
    private final String method;
    private final JsonElement params;

    protected JsonRpcBaseRequestMessage(AbstractRequestBuilder<?, ?> builder) {
        super(builder);
        this.method = Objects.requireNonNull(builder.method);
        this.params = builder.params;
    }

    public @NonNull String getMethod() {
        return method;
    }

    public @Nullable JsonElement getParams() {
        return params;
    }

    /**
     * This method can be used to convert the params to an object of the
     * specified class. This method works when the param is a single JSON
     * object.
     *
     * @param cls The class to convert the params to.
     * @return The params as an object of the specified class
     * @throws JsonRpcException If the params do not match the specified class.
     */
    public <T> T getParamsAsObject(Class<T> cls) throws JsonRpcException {
        return convertJsonElementToClass(getParams(), cls);
    }

    /**
     * This method can be used to convert one element of the params to an object
     * of the specified class. This method works when the param is an array of
     * JSON objects.
     *
     * @param index The index to get the object from.
     * @param cls The class to convert the params to.
     * @return The params as an object of the specified class
     * @throws JsonRpcException If the element in the params does not match the
     *             specified class.
     */
    public <T> T getParamsAtIndexAsObject(int index, Class<T> cls) throws JsonRpcException {
        if (params.isJsonArray()) {
            JsonArray paramArray = params.getAsJsonArray();
            if (index < paramArray.size()) {
                return convertJsonElementToClass(paramArray.get(index), cls);
            }
        } else if (index == 0) {
            return convertJsonElementToClass(params, cls);
        }
        return null;
    }

    protected abstract static class AbstractRequestBuilder<T extends AbstractRequestBuilder<T, M>,
        M extends JsonRpcBaseRequestMessage> extends AbstractBuilder<T, M> {
        private String method;
        private JsonElement params;

        public AbstractRequestBuilder() {
            // default no-args ctor
        }

        public AbstractRequestBuilder(M copyFrom) {
            super(copyFrom);
            this.params = copyFrom.getParams();
            this.method = copyFrom.getMethod();
        }

        public T method(String value) {
            this.method = value;
            return self();
        }

        public T params(JsonElement value) {
            this.params = value;
            return self();
        }

        public T paramsFromObject(Object obj) {
            return params(obj != null ? convertClassToJsonElement(obj) : null);
        }
    }


    @Override
    public String toString() {
        return "JsonRpcBaseRequestMessage [method=" + method + ", params=" + params + ", jsonrpc=" + getJsonrpc()
                + ", metadata=" + getMetadata() + ", id=" + getId() + ", type=" + getType() + "]";
    }
}
