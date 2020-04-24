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

public class DataOperationArgument extends TxOperationArgument {
    private final JsonElement data;

    @ConstructorProperties({ "txid", "store", "entity", "path", "data" })
    public DataOperationArgument(String txid, String store, String entity, JsonElement path, JsonElement data) {
        super(txid, store, entity, path);
        this.data = data;
    }

    public JsonElement getData() {
        return data;
    }
}
