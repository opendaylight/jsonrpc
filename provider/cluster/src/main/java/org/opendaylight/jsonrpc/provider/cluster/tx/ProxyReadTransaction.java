/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.tx;

import akka.util.Timeout;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

public class ProxyReadTransaction extends ProxyReadWriteTransaction implements DOMDataTreeReadTransaction {

    public ProxyReadTransaction(final Peer id, final Future<Object> actorFuture,
            final ExecutionContext executionContext, final Timeout askTimeout) {
        super(id, actorFuture, executionContext, askTimeout);
    }

    @Override
    public void close() {
        cancel();
    }
}