/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import com.google.gson.JsonElement;

import java.beans.ConstructorProperties;

/**
 * Purpose of this DTO class is to verify correct serialization of NULL element within JSON array.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Sep 26, 2019
 */
public class TestDto {
    private final JsonElement data;

    @ConstructorProperties("data")
    public TestDto(JsonElement data) {
        this.data = data;
    }

    public JsonElement getData() {
        return data;
    }
}
