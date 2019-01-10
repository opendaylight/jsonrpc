/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.Map;

/**
 * API to test deserialization of various method return types.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jan 10, 2019
 */
public interface GenericService extends AutoCloseable {
    int primitive1();

    String primitive2();

    JsonPrimitive jsonPrimitive();

    JsonArray jsonArray();

    int[] primitiveArray();

    InputObject[] objectArray();

    JsonObject jsonObject();

    List<InputObject> genericList();

    List<Map<String, InputObject>> genericMap();
}
