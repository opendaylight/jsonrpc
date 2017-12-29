/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.util.Iterator;
import java.util.ServiceLoader;
import org.opendaylight.jsonrpc.bus.BusSession;
import org.opendaylight.jsonrpc.bus.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.spi.BusSessionFactoryProvider;

/**
 * Implementation of {@link BusSessionFactoryProvider} using Thread-context
 * {@link ClassLoader}, used in non-OSGi environments. Proper function of this
 * factory provider requires Java's {@link ServiceLoader} facility.
 *
 * <p>
 * For more info about TCCL, see
 * {@link Thread#setContextClassLoader(ClassLoader)}
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public final class TcclBusSessionFactoryProvider implements BusSessionFactoryProvider {
    private static final BusSessionFactoryProvider INSTANCE = new TcclBusSessionFactoryProvider();

    private TcclBusSessionFactoryProvider() {
        // don't need to create instance every time, just share one via static
        // factory method
    }

    /**
     * Get shared instance of {@link BusSessionFactoryProvider}.
     *
     * @return reusable instance of {@link BusSessionFactoryProvider}
     */
    public static BusSessionFactoryProvider getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T extends BusSession> Iterator<BusSessionFactory<T>> getBusSessionFactories() {
        return (Iterator) ServiceLoader.load(BusSessionFactory.class).iterator();
    }
}
