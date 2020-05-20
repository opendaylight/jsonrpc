/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * DOM Rpc invoker.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 27, 2019
 */
public interface RemoteRpcInvoker {
    /**
     * Invoke RPC method and return output.
     *
     * @param name RP method name (can be prefixed by module)
     * @param rpcInput RPC input data
     * @return RPC output data
     */
    JsonElement invokeRpc(String name, JsonObject rpcInput);
}
