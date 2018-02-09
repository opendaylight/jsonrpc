/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

import java.util.Iterator;

/**
 * Service provider interface (SPI) for {@link BusSessionFactory}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public interface BusSessionFactoryProvider {
    /**
     * Return {@link Iterator} over registered {@link BusSessionFactory}
     * instances registered in system.
     *
     * @return Iterator over {@link BusSessionFactory} instances.
     */
    Iterator<BusSessionFactory> getBusSessionFactories();
}
