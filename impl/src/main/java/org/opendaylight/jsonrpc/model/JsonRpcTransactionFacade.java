/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import org.opendaylight.jsonrpc.impl.JsonRPCTx;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;

/**
 * Facade for {@link JsonRPCTx} to allow proxing.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 26, 2019
 */
public interface JsonRpcTransactionFacade extends DOMDataTreeReadWriteTransaction, DOMDataTreeReadTransaction {
    /**
     * Register listener with this transaction.
     *
     * @param listener instance of {@link TransactionListener} to register
     * @return {@link AutoCloseable} that can be used to remove provided listener.
     */
    AutoCloseable addCallback(TransactionListener listener);
}
