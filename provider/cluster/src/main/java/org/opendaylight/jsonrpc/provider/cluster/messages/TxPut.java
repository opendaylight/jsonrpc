/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.messages;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

public class TxPut extends TxBase {
    private static final long serialVersionUID = 1L;

    public TxPut(LogicalDatastoreType store, PathAndDataMsg message) {

    }

}
