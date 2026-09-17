/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion;

public class PathAndDataMsg implements Externalizable {
    private YangInstanceIdentifier path;
    private NormalizedNode data;

    public PathAndDataMsg() {
        // default ctor is required
    }

    public PathAndDataMsg(final ContainerNode data) {
        this(YangInstanceIdentifier.of(), data);
    }

    public PathAndDataMsg(@NonNull YangInstanceIdentifier path, @NonNull NormalizedNode data) {
        this.path = path;
        this.data = data;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    public NormalizedNode getData() {
        return data;
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        try (var stream = NormalizedNodeStreamVersion.POTASSIUM.newDataOutput(out)) {
            stream.writeNormalizedNode(data);
            stream.writeYangInstanceIdentifier(path);
        }
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        final var stream = NormalizedNodeDataInput.newDataInput(in);
        data = stream.readNormalizedNode();
        path = stream.readYangInstanceIdentifier();
    }

    @Override
    public String toString() {
        return "PathAndDataMsg [path=" + path + ", data=" + data + "]";
    }
}
