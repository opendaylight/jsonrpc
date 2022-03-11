/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster;

import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMNotificationPublishService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.DOMMountPointServiceImpl;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.mdsal.dom.broker.DOMRpcRouter;

public class TestCustomizer extends AbstractConcurrentDataBrokerTest {
    private final DOMMountPointServiceImpl mountPointService = new DOMMountPointServiceImpl();
    private final DOMRpcRouter rpcRouter = new DOMRpcRouter();
    private final DOMNotificationRouter notificationRouter = new DOMNotificationRouter(1);

    public TestCustomizer() {
        super(true);
    }

    public DOMMountPointService getDOMMountPointService() {
        return mountPointService;
    }

    public DOMSchemaService getSchemaService() {
        return getDataBrokerTestCustomizer().getSchemaService();
    }

    public DOMNotificationPublishService getDOMNotificationRouter() {
        return notificationRouter;
    }

    public DOMRpcRouter getDOMRpcRouter() {
        return rpcRouter;
    }
}
