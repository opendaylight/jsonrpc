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

/**
 * Implementation of {@link BusSessionFactoryProvider} used in OSGi environment.
 * This requires blueprint container as specified by OSGi Compendium R4.2, more
 * specifically in 121.7.3 "reference-list" manager.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class OsgiBusSessionFactoryProvider implements BusSessionFactoryProvider {
    private final List<BusSessionFactory> sessionFactories;

    public OsgiBusSessionFactoryProvider(List<BusSessionFactory> sessionFactories) {
        this.sessionFactories = sessionFactories;
    }

    @Override
    public Iterator<BusSessionFactory> getBusSessionFactories() {
        return sessionFactories.iterator();
    }
}
