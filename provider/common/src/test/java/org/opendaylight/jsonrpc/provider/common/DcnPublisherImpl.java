/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import java.util.concurrent.CountDownLatch;
import org.opendaylight.jsonrpc.model.DataChangeNotification;
import org.opendaylight.jsonrpc.model.DataChangeNotificationPublisher;

public class DcnPublisherImpl implements DataChangeNotificationPublisher {
    private final CountDownLatch latch;

    public DcnPublisherImpl(final CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public void close() throws Exception {
        // NOOP
    }

    @Override
    public void notifyListener(DataChangeNotification change) {
        latch.countDown();
    }
}
