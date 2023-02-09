/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.Map;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Source of {@link EventLoopGroup}s for {@link SCRIntegration}.
 */
@Component(factory = SCREventLoopGroup.FACTORY_NAME, service = EventLoopGroup.class)
public final class SCREventLoopGroup extends NioEventLoopGroup {
    static final String FACTORY_NAME = "org.opendaylight.jsonrpc.bus.spi.SCREventLoopGroup";

    @Activate
    public SCREventLoopGroup(Map<String, ?> properties) {
        super(SCRIntegration.size(properties), SCRIntegration.threadFactory(properties));
    }

    @Deactivate
    public void deactivate() {
        super.shutdown();
    }
}
