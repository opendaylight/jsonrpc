/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.http;

import org.opendaylight.jsonrpc.bus.api.BusSessionFactory;
import org.opendaylight.jsonrpc.bus.spi.EventLoopConfiguration;
import org.opendaylight.jsonrpc.security.api.SecurityService;
import org.opendaylight.jsonrpc.security.noop.NoopSecurityService;


/**
 * {@link BusSessionFactory} implemented using secured websocket channel.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 9, 2018
 */
public class WssBusSessionFactory extends AbstractWebBusSessionFactory {
    public WssBusSessionFactory() {
        super("wss", true, true, 443);
    }

    public WssBusSessionFactory(EventLoopConfiguration config, SecurityService securityService) {
        super("wss", true, true, 443, config, securityService);
    }

    public WssBusSessionFactory(EventLoopConfiguration config) {
        this(config, NoopSecurityService.INSTANCE);
    }
}
