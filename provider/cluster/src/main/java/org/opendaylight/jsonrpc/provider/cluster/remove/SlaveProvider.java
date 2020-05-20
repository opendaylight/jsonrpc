/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.remove;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlaveProvider implements ClusteredDataTreeChangeListener<ActualEndpoints>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SlaveProvider.class);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    public SlaveProvider() {

    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<ActualEndpoints>> changes) {

    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {

        }
    }

    public void enable() {
        if (closed.get()) {
            throw new IllegalStateException("Can't enable provider that was closed already");
        }
    }

    public void disable() {

    }
}
