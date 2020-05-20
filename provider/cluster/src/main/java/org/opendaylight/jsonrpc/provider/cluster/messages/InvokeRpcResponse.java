/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.messages;

import java.io.Serializable;
import java.util.Collection;
import org.opendaylight.yangtools.yang.common.RpcError;

public class InvokeRpcResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    public Collection<? extends RpcError> getErrors() {
        // TODO Auto-generated method stub
        return null;
    }

    public PathAndDataMsg getResponseData() {
        // TODO Auto-generated method stub
        return null;
    }

}
