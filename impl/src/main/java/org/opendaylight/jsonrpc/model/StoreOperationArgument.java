/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import com.google.gson.JsonElement;
import java.beans.ConstructorProperties;

public class StoreOperationArgument {
    private final String store;
    private final String entity;
    private final JsonElement path;

    @ConstructorProperties({ "store", "entity", "path" })
    public StoreOperationArgument(String store, String entity, JsonElement path) {
        this.store = store;
        this.entity = entity;
        this.path = path;
    }

    public String getStore() {
        return store;
    }

    public String getEntity() {
        return entity;
    }

    public JsonElement getPath() {
        return path;
    }
}
