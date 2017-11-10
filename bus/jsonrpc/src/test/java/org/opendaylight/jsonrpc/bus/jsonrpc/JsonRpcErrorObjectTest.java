/*
 * Copyright (c) 2017 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.jsonrpc.bus.jsonrpc;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

/*
 * Verify constructors and getters in JsonRpcErrorObject class
 */
public class JsonRpcErrorObjectTest {
    private final int testCode = -32000;
    private final String testMessage = "Error Message";

    @Test
    public void testValidCreate() {
        JsonRpcErrorObject errObj = new JsonRpcErrorObject(testCode, testMessage, null);
        assertEquals(testCode, errObj.getCode());
        assertEquals(testMessage, errObj.getMessage());
        assertNull(errObj.getData());
    }

    @Test
    public void testValidCreateViaObject() {
        // Create JsonObject to mimic an incoming valid message
        JsonObject obj = new JsonObject();
        obj.add("code", new JsonPrimitive(testCode));
        obj.add("message", new JsonPrimitive(testMessage));
        JsonRpcErrorObject errObj = new JsonRpcErrorObject(obj);
        assertEquals(testCode, errObj.getCode());
        assertEquals(testMessage, errObj.getMessage());
        assertNull(errObj.getData());
    }

    @Test
    public void testIncompleteCreateViaObject() {
        // Create empty JsonObject to mimic an invalid incoming message
        JsonObject obj = new JsonObject();
        JsonRpcErrorObject errObj = new JsonRpcErrorObject(obj);
        assertEquals(0, errObj.getCode());
        assertNull(errObj.getMessage());
        assertNull(errObj.getData());
    }
}
