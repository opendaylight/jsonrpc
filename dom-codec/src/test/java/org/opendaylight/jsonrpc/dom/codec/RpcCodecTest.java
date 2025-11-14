/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

public class RpcCodecTest extends AbstractCodecTest {
    private static final JsonElement F_INPUT = JsonParser.parseString("{ \"in-number\": 5 }");

    @Test
    public void testInputBasic() throws IOException {
        final JsonObject input = F_INPUT.getAsJsonObject();
        final Codec<JsonElement, ContainerNode, IOException> codec = factory.rpcInputCodec(getRpc("factorial"));
        final ContainerNode decoded = codec.deserialize(input);
        JsonElement encoded = codec.serialize(decoded);
        assertThat(encoded.toString(), hasJsonPath("$.in-number", equalTo(5)));
        assertEquals(input, encoded);
    }

    @Test
    public void testIntputNull() throws IOException {
        final Codec<JsonElement, ContainerNode, IOException> codec = factory.rpcInputCodec(getRpc("factorial"));
        ContainerNode result = codec.deserialize(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        result = codec.deserialize(JsonNull.INSTANCE);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInputArray() throws IOException {
        final Codec<JsonElement, ContainerNode, IOException> codec = factory.rpcInputCodec(getRpc("factorial"));
        final JsonArray arr = new JsonArray();
        arr.add(6);
        ContainerNode decoded = codec.deserialize(arr);
        JsonElement encoded = codec.serialize(decoded);
        assertThat(encoded.toString(), hasJsonPath("$.in-number", equalTo(6)));
    }

    @Test
    public void testInputArrayMismatchArguments() throws IOException {
        final Codec<JsonElement, ContainerNode, IOException> codec = factory.rpcInputCodec(getRpc("factorial"));
        final JsonArray arr = new JsonArray();
        arr.add(1);
        arr.add(2);
        arr.add(3);
        final var ex = assertThrows(IllegalArgumentException.class, () -> codec.deserialize(arr));
        assertEquals("Number of input array elements (3) does not match number of child schema nodes (1)",
            ex.getMessage());
    }

    @Test
    public void testOutputNull() throws IOException {
        final Codec<JsonElement, ContainerNode, IOException> codec = factory.rpcOutputCodec(getRpc("factorial"));
        ContainerNode result = codec.deserialize(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testInputPrimitive() throws IOException {
        JsonElement input = JsonParser.parseString("5");
        final Codec<JsonElement, ContainerNode, IOException> codec = factory.rpcInputCodec(getRpc("factorial"));
        ContainerNode decoded = codec.deserialize(input);
        JsonElement encoded = codec.serialize(decoded);
        assertEquals(F_INPUT, encoded);
    }

    private RpcDefinition getRpc(final String name) {
        return schemaContext.getOperations()
                .stream()
                .filter(rpc -> rpc.getQName().getLocalName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
