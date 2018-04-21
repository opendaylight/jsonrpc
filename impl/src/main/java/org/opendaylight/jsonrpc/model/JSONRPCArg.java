/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import com.google.gson.JsonElement;

/**
 * Holder of path + data pair.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class JSONRPCArg {
    private final JsonElement path;
    private final JsonElement data;

    public JSONRPCArg(JsonElement path, JsonElement data) {
        this.path = path;
        this.data = data;
    }

    public JsonElement getPath() {
        return path;
    }

    public JsonElement getData() {
        return data;
    }
}
