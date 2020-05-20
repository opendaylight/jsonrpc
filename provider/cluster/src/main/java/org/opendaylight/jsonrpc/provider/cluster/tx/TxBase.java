/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.tx;

import java.io.Serializable;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

/**
 * Base class of data operation messages.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 13, 2020
 */
public class TxBase implements Serializable {
    private static final long serialVersionUID = 1L;

    protected LogicalDatastoreType store;

    public TxBase(LogicalDatastoreType store) {
        this.store = store;
    }

    public LogicalDatastoreType getStore() {
        return store;
    }
}
