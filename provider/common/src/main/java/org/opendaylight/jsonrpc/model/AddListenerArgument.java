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

public class AddListenerArgument extends StoreOperationArgument {
    private final String transport;

    @ConstructorProperties({ "store", "entity", "path", "transport" })
    public AddListenerArgument(String store, String entity, JsonElement path, String transport) {
        super(store, entity, path);
        this.transport = transport;
    }

    public String getTransport() {
        return transport;
    }
}
