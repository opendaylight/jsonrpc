/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class PathCodecAndBuilderTest extends AbstractCodecTest {
    @Test
    public void testEncodeMultilevelListPath() {
        final JsonObject jsonPath = JsonRpcPathBuilder.newBuilder("network-topology:network-topology")
                .container("topology")
                .item("topology-id", "topo-1")
                .container("node")
                .item("node-id", "node-1")
                .container("termination-point")
                .item("tp-id", "eth0")
                .build();

        LOG.info("IN:{}", jsonPath);
        final YangInstanceIdentifier yii = factory.pathCodec().deserialize(jsonPath);
        LOG.info("Result : {}", yii);
        assertEquals(7, yii.getPathArguments().size());
    }
}
