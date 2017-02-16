/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.jsonrpc;

import java.util.Arrays;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcException;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcMessageIntArrayParams {
    public static final Logger logger = LoggerFactory.getLogger(RpcMessageIntArrayParams.class);
    JsonRpcRequestMessage req;
    int[] params;

    public RpcMessageIntArrayParams(JsonRpcRequestMessage req) {
        this.req = req;
        try {
            this.params = req.getParamsAsObject(int[].class);
        } catch (JsonRpcException e) {
            logger.error("JSON message error", e);
        }
    }

    public int[] getParams() {
        return params;
    }

    @Override
    public String toString() {
        return "RpcMessageIntArrayParams [req=" + req + ", params=" + Arrays.toString(params) + "]";
    }
}
