/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.messages;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ImmutableList.Builder;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class SchemaPathMsg implements Externalizable {
    private static final long serialVersionUID = 1L;
    private SchemaPath path;

    public SchemaPathMsg(SchemaPath path) {
        this.path = path;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        final Iterable<QName> qnames = path.getPathFromRoot();
        out.writeInt(Iterables.size(qnames));
        for (final QName qname : qnames) {
            qname.writeTo(out);
        }
        out.writeBoolean(path.isAbsolute());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        final boolean absolute = in.readBoolean();
        final int size = in.readInt();
        final Builder<QName> qnames = ImmutableList.builderWithExpectedSize(size);
        for (int i = 0; i < size; ++i) {
            qnames.add(QName.readFrom(in));
        }
        path = SchemaPath.create(qnames.build(), absolute);
    }

    public SchemaPath getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "SchemaPathMsg [path=" + path + "]";
    }
}
