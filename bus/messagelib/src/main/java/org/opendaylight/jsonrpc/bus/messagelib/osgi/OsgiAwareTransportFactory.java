/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib.osgi;

import org.opendaylight.jsonrpc.bus.api.BusSessionFactoryProvider;
import org.opendaylight.jsonrpc.bus.messagelib.AbstractTransportFactory;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Implementation of {@link TransportFactory} which requires semantics of
 * {@link OsgiBusSessionFactoryProvider}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
@Component(service = TransportFactory.class)
public class OsgiAwareTransportFactory extends AbstractTransportFactory {
    @Activate
    public OsgiAwareTransportFactory(@Reference BusSessionFactoryProvider busSessionFactoryProvider) {
        super(busSessionFactoryProvider);
    }
}
