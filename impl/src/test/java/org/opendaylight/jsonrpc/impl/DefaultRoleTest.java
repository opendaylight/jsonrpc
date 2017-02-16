/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumHashMap;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.hmap.JsonPathCodec;
import org.opendaylight.jsonrpc.model.JSONRPCArg;
import org.opendaylight.jsonrpc.model.RemoteOmShard;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Test to ensure that default role is appended to URI's query string, when none
 * was provided from user input.
 * 
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class DefaultRoleTest {
    private JsonRPCTx tx;
    private SchemaContext schemaContext;
    private JsonConverter jsonConverter;
    private TransportFactory transportFactory;
    private HierarchicalEnumMap<JsonElement, DataType, String> pm;

    @Before
    public void setUp() {
        pm = HierarchicalEnumHashMap.create(DataType.class, JsonPathCodec.create());
        transportFactory = mock(TransportFactory.class);
        jsonConverter = mock(JsonConverter.class);
        schemaContext = mock(SchemaContext.class);
        tx = new JsonRPCTx(transportFactory, "test", pm, jsonConverter, schemaContext);
    }

    @Test
    public void test() throws URISyntaxException {
        doAnswer(invocation -> {
            final String endpoint = (String) invocation.getArguments()[1];
            assertNotEquals(-1, endpoint.indexOf("role=REQ"));
            return mock(RemoteOmShard.class);
        }).when(transportFactory).createProxy(any(), any());
        pm.put(new JsonObject(), DataType.CONFIGURATION_DATA, "zmq://localhost:12345");
        JSONRPCArg arg = new JSONRPCArg(new JsonObject(), new JsonObject());
        doReturn(arg).when(jsonConverter).convert(any(YangInstanceIdentifier.class), any());
        tx.read(LogicalDatastoreType.CONFIGURATION,
                YangInstanceIdentifier.builder().node(NetworkTopology.QNAME).build());
    }
}
