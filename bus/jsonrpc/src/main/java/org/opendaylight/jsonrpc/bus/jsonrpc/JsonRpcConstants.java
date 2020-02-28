/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public final class JsonRpcConstants {
    private JsonRpcConstants() {
        // no instantiation
    }

    static final String JSONRPC = "jsonrpc";
    static final String METHOD = "method";
    static final String PARAMS = "params";
    static final String ID = "id";
    static final String RESULT = "result";
    static final String ERROR = "error";
    static final String CODE = "code";
    static final String MESSAGE = "message";
    static final String DATA = "data";
    static final String METADATA = "metadata";

    private static final Set<Class<?>> PRIMITIVE_TYPES_AND_STRING = ImmutableSet.<Class<?>>of(int.class, long.class,
            short.class, float.class, double.class, byte.class, boolean.class, char.class, Integer.class, Long.class,
            Short.class, Float.class, Double.class, Byte.class, Boolean.class, Character.class, String.class);

    public static boolean canRepresentJsonPrimitive(Class<?> clazz) {
        return PRIMITIVE_TYPES_AND_STRING.contains(clazz);
    }
}
