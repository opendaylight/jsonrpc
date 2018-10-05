/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq.interop;

import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.spi.AbstractSessionTest;
import org.opendaylight.jsonrpc.bus.zmq.ZmqBusSessionFactory;

abstract class AbstractInteropTest extends AbstractSessionTest {
    @Override
    protected BusSessionFactory createFactory() {
        return new ZmqBusSessionFactory(config);
    }
}
