/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.PeerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link MessageListener} which discard inbound message.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 7, 2018
 */
public final class DiscardingMessageListener implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(DiscardingMessageListener.class);
    public static final DiscardingMessageListener INSTANCE = new DiscardingMessageListener();

    private DiscardingMessageListener() {
        // prevent others to create more instances
    }

    @Override
    public void onMessage(PeerContext peerContext, String message) {
        LOG.debug("Discarding inbound message {}", message);
    }
}
