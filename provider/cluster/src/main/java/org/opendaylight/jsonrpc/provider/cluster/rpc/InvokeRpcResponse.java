/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.rpc;

import java.io.Serializable;
import java.util.Collection;
import org.opendaylight.jsonrpc.provider.cluster.messages.PathAndDataMsg;
import org.opendaylight.yangtools.yang.common.RpcError;

/**
 * Response of RPC invocation.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 13, 2020
 */
public class InvokeRpcResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private PathAndDataMsg data;
    private Collection<? extends RpcError> errors;

    public InvokeRpcResponse(PathAndDataMsg data, Collection<? extends RpcError> errors) {
        this.data = data;
        this.errors = errors;
    }

    public Collection<? extends RpcError> getErrors() {
        return errors;
    }

    public PathAndDataMsg getData() {
        return data;
    }

    @Override
    public String toString() {
        return "InvokeRpcResponse [data=" + data + ", errors=" + errors + "]";
    }
}
