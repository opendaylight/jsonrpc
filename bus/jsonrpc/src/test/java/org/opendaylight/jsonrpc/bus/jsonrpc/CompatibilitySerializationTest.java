/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.List;
import org.junit.Test;

/**
 * Test some compatibility-related scenarios. There are 2 possible forms of serialization format. Consider following
 * java code:
 *
 * <p>
 * <code>
 * public class ModuleInfo {
 *     String name;
 *     String revision;
 * }
 *
 * public List&lt;ModuleInfo&gt; getModules() { ... }
 * </code>
 *
 * <p>
 * These JSONRPC responses should be considered same with regards to resulting object:
 *
 * <p>
 * <code>
 * { "jsonrpc" : "2.0", "id" : 1, "result" : [{"name":"abc", "revision":"2020-02-29"}] }
 * </code>
 *
 * <p>
 * and
 *
 * <p>
 * <code>
 * { "jsonrpc" : "2.0", "id" : 1, "result" : { "modules" : [{"name":"abc", "revision":"2020-02-29"}] } }
 * </code>
 *
 * <p>
 * Same rules applies for JSONRPC parameters element.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 29, 2020
 */
public class CompatibilitySerializationTest {
    public final class ModuleInfo {
        String module;
        String revision;
    }

    public static class TestArr {
        private List<String> arr;

        public void setArr(List<String> arr) {
            this.arr = arr;
        }

        public List<String> getArr() {
            return arr;
        }
    }

    public static class TestObj {
        private boolean success = false;

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    @Test
    public void testArrayToObject() throws JsonRpcException {
        JsonArray arr = new JsonArray();
        arr.add("a");
        arr.add("b");
        arr.add("c");
        final JsonRpcReplyMessage reply = JsonRpcReplyMessage.builder().result(arr).build();
        TestArr result = reply.getResultAsObject(TestArr.class);
        assertEquals(3, result.getArr().size());
    }

    @Test
    public void testObjectToArray() throws JsonRpcException {
        JsonObject payload = new JsonObject();
        JsonArray arr = new JsonArray();
        payload.add("arr", arr);
        arr.add("a");
        arr.add("b");
        arr.add("c");
        final JsonRpcRequestMessage req = JsonRpcRequestMessage.builder()
                .idFromIntValue(1)
                .method("test")
                .params(payload)
                .build();
        @SuppressWarnings("unchecked")
        List<String> result = req.getParamsAsObject(List.class);
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @SuppressWarnings("serial")
    @Test
    public void testObjectToGenericList() throws JsonRpcException {
        String json = "{\"modules\":[{\"module\":\"ietf-inet-types\",\"revision\""
                + ":\"2013-07-15\"},{\"module\":\"abc-model\"}]}";
        JsonRpcReplyMessage reply = JsonRpcReplyMessage.builder().result(JsonParser.parseString(json)).build();
        List<ModuleInfo> list = reply.getResultAsObject(new TypeToken<List<ModuleInfo>>() {
        }.getType());
        assertEquals(2, list.size());
        assertEquals("abc-model", list.get(1).module);
    }

    @Test
    public void testPrimitiveToObject() throws JsonRpcException {
        final JsonRpcReplyMessage reply = JsonRpcReplyMessage.builder().result(new JsonPrimitive(true)).build();
        final TestObj result = reply.getResultAsObject(TestObj.class);
        assertTrue(result.isSuccess());
    }

    @Test
    public void testObjectToPrimitive() throws JsonRpcException {
        JsonObject payload = new JsonObject();
        payload.add("success", new JsonPrimitive(true));
        final JsonRpcRequestMessage req = JsonRpcRequestMessage.builder()
                .idFromIntValue(1)
                .method("test")
                .params(payload)
                .build();
        boolean result = req.getParamsAsObject(Boolean.class);
        assertTrue(result);
    }
}
