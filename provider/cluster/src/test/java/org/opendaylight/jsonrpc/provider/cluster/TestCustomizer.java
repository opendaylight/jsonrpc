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
import org.opendaylight.mdsal.dom.broker.RouterDOMPublishNotificationService;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public class TestCustomizer extends AbstractConcurrentDataBrokerTest {
    private final DOMMountPointServiceImpl mountPointService = new DOMMountPointServiceImpl();
    private final DOMNotificationRouter notificationRouter = new DOMNotificationRouter(1);

    private DOMRpcRouter rpcRouter;

    public TestCustomizer() {
        super(true);
    }

    @Override
    protected void setupWithSchema(final EffectiveModelContext context) {
        super.setupWithSchema(context);
        rpcRouter = new DOMRpcRouter(getSchemaService());
    }

    public DOMMountPointService getDOMMountPointService() {
        return mountPointService;
    }

    public DOMSchemaService getSchemaService() {
        return getDataBrokerTestCustomizer().getSchemaService();
    }

    public DOMNotificationPublishService getDOMNotificationRouter() {
        return new RouterDOMPublishNotificationService(notificationRouter);
    }

    public DOMRpcRouter getDOMRpcRouter() {
        return rpcRouter;
    }
}
