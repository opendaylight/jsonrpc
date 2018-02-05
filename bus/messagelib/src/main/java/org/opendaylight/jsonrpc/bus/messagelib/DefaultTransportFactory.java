/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

/**
 * Default implementation of {@link TransportFactory} normally used in
 * standalone applications and tests.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public class DefaultTransportFactory extends AbstractTransportFactory {
    public DefaultTransportFactory() {
        super(TcclBusSessionFactoryProvider.getInstance());
    }
}
