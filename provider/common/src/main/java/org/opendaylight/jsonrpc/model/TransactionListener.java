/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;

/**
 * Transaction lifecycle change listener.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jun 20, 2018
 */
public interface TransactionListener {
    /**
     * Called when transaction is cancelled.
     *
     * @param jsonRPCTx transaction being cancelled
     */
    void onCancel(DOMDataTreeWriteTransaction jsonRPCTx);

    /**
     * Called upon successful commit.
     *
     * @param jsonRPCTx transaction being successfully committed.
     */
    void onSuccess(DOMDataTreeWriteTransaction jsonRPCTx);

    /**
     * Called when commit failed.
     *
     * @param jsonRPCTx transaction in which failure occur
     * @param failure cause of problem
     */
    void onFailure(DOMDataTreeWriteTransaction jsonRPCTx, Throwable failure);

    /**
     * Called just before transaction is being committed.
     *
     * @param jsonRPCTx transaction being committed
     */
    void onSubmit(DOMDataTreeWriteTransaction jsonRPCTx);
}
