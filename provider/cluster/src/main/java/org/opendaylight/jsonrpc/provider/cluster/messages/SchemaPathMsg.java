/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.messages;

import com.google.common.collect.Iterables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Message that conveys {@link SchemaPath}.
 *
 * <p>
 * Acknowledgement : this code is inspired by implementation of netconf-topology-singleton.
 *
 * @since Jul 14, 2020
 */
public class SchemaPathMsg implements Serializable {
    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings("SE_BAD_FIELD")
    private final SchemaPath schemaPath;

    public SchemaPathMsg(final SchemaPath schemaPath) {
        this.schemaPath = schemaPath;
    }

    public SchemaPath getSchemaPath() {
        return schemaPath;
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    @Override
    public String toString() {
        return "SchemaPathMsg [schemaPath=" + schemaPath + "]";
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 2L;

        private SchemaPathMsg schemaPathMsg;

        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // due to Externalizable
        }

        Proxy(final SchemaPathMsg schemaPathMsg) {
            this.schemaPathMsg = schemaPathMsg;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            final Iterable<QName> path = schemaPathMsg.getSchemaPath().getPathFromRoot();
            out.writeInt(Iterables.size(path));
            for (final QName qualifiedName : path) {
                out.writeObject(qualifiedName);
            }
            out.writeBoolean(schemaPathMsg.getSchemaPath().isAbsolute());
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final int sizePath = in.readInt();
            final QName[] paths = new QName[sizePath];
            for (int i = 0; i < sizePath; i++) {
                paths[i] = (QName) in.readObject();
            }
            final boolean absolute = in.readBoolean();
            schemaPathMsg = new SchemaPathMsg(SchemaPath.create(absolute, paths));
        }

        private Object readResolve() {
            return schemaPathMsg;
        }
    }
}
