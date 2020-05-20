/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.messages;

import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMRpcService;

/**
 * Sent by master to self to setup mountpoint services.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 16, 2020
 */
public class InitMasterMountPoint {
    private final DOMDataBroker domDataBroker;
    private final DOMRpcService domRpcService;

    public InitMasterMountPoint(DOMDataBroker domDataBroker, DOMRpcService domRpcService) {
        this.domDataBroker = domDataBroker;
        this.domRpcService = domRpcService;
    }

    public DOMDataBroker getDomDataBroker() {
        return domDataBroker;
    }

    public DOMRpcService getDomRpcService() {
        return domRpcService;
    }
}
