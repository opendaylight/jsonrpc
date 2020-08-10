/*
 * Copyright (c) 2020 dNation.tech. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.io.IOException;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DataCodecTest extends AbstractCodecTest {
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyPath() {
        factory.dataCodec(YangInstanceIdentifier.empty());
    }

    @Test
    public void testContainer() throws IOException {
        JsonElement parsed = loadJsonData("topo3.json");
        final YangInstanceIdentifier path = factory.pathCodec()
                .deserialize(JsonRpcPathBuilder.newBuilder("network-topology:network-topology").build());

        final NormalizedNode<?, ?> data = factory.dataCodec(path).deserialize(parsed);
        dumpNormalizedNode(data);

        JsonElement encoded = factory.dataCodec(path).serialize(data);
        assertEquals(parsed, encoded);
    }

    @Test
    public void testDecodeNull() throws IOException {
        final YangInstanceIdentifier path = factory.pathCodec()
                .deserialize(JsonRpcPathBuilder.newBuilder("network-topology:network-topology").build());
        assertNull(factory.dataCodec(path).deserialize(null));
        assertNull(factory.dataCodec(path).deserialize(JsonNull.INSTANCE));
    }

    @Test
    public void testEncodeNull() throws IOException {
        final YangInstanceIdentifier path = factory.pathCodec()
                .deserialize(JsonRpcPathBuilder.newBuilder("network-topology:network-topology").build());
        assertNull(factory.dataCodec(path).serialize(null));
    }

    @Test
    public void testListItem() throws IOException {
        JsonElement parsed = loadJsonData("topo1.json");
        final YangInstanceIdentifier path = factory.pathCodec()
                .deserialize(JsonRpcPathBuilder.newBuilder("network-topology:network-topology")
                        .container("topology")
                        .item("topology-id", "topo-id")
                        .build());

        dumpYangPath(path);
        final NormalizedNode<?, ?> data = factory.dataCodec(path).deserialize(parsed);
        dumpNormalizedNode(data);

        JsonElement encoded = factory.dataCodec(path).serialize(data);
        assertEquals(parsed, encoded);
    }

    @Test
    public void testEncodeContainer() throws IOException {
        YangInstanceIdentifier path = YangInstanceIdentifier.of(NetworkTopology.QNAME);
        dumpYangPath(path);
        NormalizedNode<?, ?> data = loadDomData("topo2.json", path);
        // Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> data = mockTopology();

        final JsonObject jsonPath = factory.pathCodec().serialize(path);
        final JsonElement result = factory.dataCodec(path).serialize(data);

        assertThat(result.toString(), hasJsonPath("$.topology[0].topology-id", equalTo("topo-id")));
        assertThat(result.toString(), hasJsonPath("$.topology[0].node", hasSize(2)));
        assertThat(jsonPath.toString(), hasJsonPath("$.network-topology:network-topology.*", hasSize(0)));
    }
}
