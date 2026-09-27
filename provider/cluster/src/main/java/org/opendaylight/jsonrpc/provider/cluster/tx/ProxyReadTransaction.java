/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.tx;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;

public class ProxyReadTransaction extends ProxyReadWriteTransaction implements DOMDataTreeReadTransaction {
    public ProxyReadTransaction(final Peer id, final CompletionStage<Object> actorFuture, final Duration askTimeout) {
        super(id, actorFuture, askTimeout);
    }

    @Override
    public void close() {
        cancel();
    }
}
