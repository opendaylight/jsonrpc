/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import org.kohsuke.MetaInfServices;
import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.spi.EventLoopConfiguration;
import org.opendaylight.jsonrpc.security.api.SecurityService;
import org.opendaylight.jsonrpc.security.noop.NoopSecurityService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;


/**
 * {@link BusSessionFactory} implemented using plain HTTP/1.1 channel.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 9, 2018
 */
@MetaInfServices(value = BusSessionFactory.class)
@Component(service = BusSessionFactory.class, property = "scheme=http")
public class HttpBusSessionFactory extends AbstractWebBusSessionFactory {
    public HttpBusSessionFactory() {
        super("http", false, false, 80);
    }

    @Activate
    public HttpBusSessionFactory(@Reference(target = "(name=jsonrpc)") EventLoopConfiguration config,
            @Reference SecurityService securityService) {
        super("http", false, false, 80, config, securityService);
    }

    public HttpBusSessionFactory(EventLoopConfiguration config) {
        this(config, NoopSecurityService.INSTANCE);
    }

    @Deactivate
    public void deactivate() {
        close();
    }
}
