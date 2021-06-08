/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.rpc;

import java.io.Serializable;
import org.opendaylight.jsonrpc.provider.cluster.messages.PathAndDataMsg;
import org.opendaylight.jsonrpc.provider.cluster.messages.SchemaPathMsg;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public class InvokeRpcRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private SchemaPathMsg schemaPath;
    private PathAndDataMsg data;

    public static InvokeRpcRequest create(Absolute path, NormalizedNode input) {
        final PathAndDataMsg data;
        if (input != null) {
            data = new PathAndDataMsg(YangInstanceIdentifier.empty(), input);
        } else {
            data = null;
        }
        return new InvokeRpcRequest(new SchemaPathMsg(path), data);
    }

    public InvokeRpcRequest() {
        // default ctor is required
    }

    public InvokeRpcRequest(SchemaPathMsg schemaPathMsg, PathAndDataMsg data) {
        this.schemaPath = schemaPathMsg;
        this.data = data;
    }

    public SchemaPathMsg getSchemaPath() {
        return schemaPath;
    }

    public PathAndDataMsg getData() {
        return data;
    }

    @Override
    public String toString() {
        return "InvokeRpcRequest [schemaPath=" + schemaPath + ", data=" + data + "]";
    }
}
