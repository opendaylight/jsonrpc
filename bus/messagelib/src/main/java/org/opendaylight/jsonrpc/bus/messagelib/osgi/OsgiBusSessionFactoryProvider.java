/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib.osgi;

import java.util.Iterator;
import java.util.List;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactoryProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Implementation of {@link BusSessionFactoryProvider} used in OSGi environment.
 * This requires blueprint container as specified by OSGi Compendium R4.2, more
 * specifically in 121.7.3 "reference-list" manager.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
@Component
public class OsgiBusSessionFactoryProvider implements BusSessionFactoryProvider {
    private final List<BusSessionFactory> sessionFactories;

    @Activate
    public OsgiBusSessionFactoryProvider(
            @Reference(target = "(scheme=http)") BusSessionFactory http,
            @Reference(target = "(scheme=https)") BusSessionFactory https,
            @Reference(target = "(scheme=ws)") BusSessionFactory ws,
            @Reference(target = "(scheme=wss)") BusSessionFactory wss,
            @Reference(target = "(scheme=zmq)") BusSessionFactory zmq) {
        this.sessionFactories = List.of(http, https, ws, wss, zmq);
    }

    @Override
    public Iterator<BusSessionFactory> getBusSessionFactories() {
        return sessionFactories.iterator();
    }
}
