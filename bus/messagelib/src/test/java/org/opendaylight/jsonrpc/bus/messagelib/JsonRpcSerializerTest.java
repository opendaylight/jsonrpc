/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertFalse;

import java.util.List;
import org.junit.Test;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcSerializer;

/**
 * Unit tests for JsonRpcSerializer.
 *
 * @author Allan Clarke
 */
public class JsonRpcSerializerTest {

    private static final String EMPTY = "";

    @Test
    public void parsingEmptyJsonShouldBeNoticed() {
        // when
        List<JsonRpcBaseMessage> result = JsonRpcSerializer.fromJson(EMPTY);
        // then
        assertFalse(result.isEmpty());
    }
}
