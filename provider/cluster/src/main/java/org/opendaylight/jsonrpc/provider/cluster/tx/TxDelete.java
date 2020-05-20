/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.tx;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class TxDelete extends TxBase {
    private static final long serialVersionUID = 1L;

    private final YangInstanceIdentifier path;

    public TxDelete(LogicalDatastoreType store, YangInstanceIdentifier path) {
        super(store);
        this.path = path;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "TxDelete [store=" + store + ", path=" + path + "]";
    }
}
