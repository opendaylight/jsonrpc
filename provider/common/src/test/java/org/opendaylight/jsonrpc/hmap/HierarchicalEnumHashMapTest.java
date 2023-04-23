/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.hmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HierarchicalEnumHashMapTest {
    private static final Logger LOG = LoggerFactory.getLogger(HierarchicalEnumHashMapTest.class);
    private static final String PATH1 = "{ \"level1\" : { \"level2\" : { \"level3b\" : {}}}}";
    private static final String PATH2 = "{ \"level1\" : { \"level2\" : { \"level3a\" : {}}}}";
    private static final JsonPathCodec CODEC = JsonPathCodec.create();

    private enum Types {
        A, B, C;
    }

    @Test
    public void testLookupPut() throws IOException {
        final HierarchicalEnumMap<JsonElement, Types, String> map = HierarchicalEnumHashMap.create(Types.class, CODEC);
        map.put(parse("{}"), Types.A, "uri://localhost");
        map.put(parse(PATH1), Types.A, "xyz");
        assertEquals(Optional.of("uri://localhost"), map.lookup(JsonParser.parseString(PATH2), Types.A));
        assertNull(map.put(parse(getData("path3")), Types.A, "HERE"));
        assertNull(map.put(parse(getData("path4")), Types.A, "another"));
        assertNull(map.put(parse(getData("path5")), Types.A, "123456"));
        assertNull(map.put(parse(getData("path5")), Types.B, "123456"));
        assertNull(map.put(parse(getData("path5")), Types.C, "123456"));
        assertEquals("123456", map.put(parse(getData("path5")), Types.A, "987654"));
        assertEquals("123456", map.put(parse(getData("path5")), Types.C, "987654"));
        assertEquals(Optional.of("987654"), map.lookup(parse(getData("path5")), Types.A));
        assertEquals(Optional.of("another"), map.lookup(parse(getData("path4")), Types.A));
        LOG.info(map.dump());
        Map<JsonElement, String> map2 = map.toMap(Types.A);
        LOG.info("{}", map2);
        assertEquals(5, map2.size());
    }

    @Test
    public void testSerializePath() throws IOException {
        String str = Iterables.toString(CODEC.serialize(parse(PATH1)));
        LOG.info("Serialized : {}", str);
        str = Iterables.toString(CODEC.serialize(parse(PATH2)));
        LOG.info("Serialized : {}", str);
        str = Iterables.toString(CODEC.serialize(parse(getData("path3"))));
        LOG.info("Serialized : {}", str);
        str = Iterables.toString(CODEC.serialize(parse(getData("path4"))));
        LOG.info("Serialized : {}", str);
        assertEquals("[network-topology:network-topology, topology, "
                + "topology-id=topology1, node, node-id=node1, termination-point, tp-id=eth0]", str);
    }

    @Test
    public void testDeserializePath() throws IOException {
        JsonElement json;
        json = CODEC.deserialize(Lists.newArrayList(null, "level1", "level2", "level3"));
        LOG.info("JSON : {}", json);
        assertEquals("{\"level1\":{\"level2\":{\"level3\":{}}}}", json.toString());
        final List<String> rootOnlyPath = new ArrayList<>();
        rootOnlyPath.add(null);
        json = CODEC.deserialize(rootOnlyPath);
        LOG.info("JSON : {}", json);
        assertEquals("{}", json.toString());
        json = CODEC.deserialize(Lists.newArrayList(null, "level1", "item1=value", "level3", "item=value"));
        LOG.info("JSON : {}", json);
        json = CODEC.deserialize(Lists.newArrayList(null, "network-topology:network-topology", "topology",
                "topology-id=topology1", "node", "node-id=node1", "termination-point", "tp-id=eth0"));
        LOG.info("JSON : {}", json);
        assertEquals(parse(getData("path4")).toString(), json.toString());
    }

    private static JsonElement parse(String str) {
        return JsonParser.parseString(str);
    }

    private String getData(String name) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = getClass().getResourceAsStream("/" + name + ".json")) {
            is.transferTo(baos);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }
}
