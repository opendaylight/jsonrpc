/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Objects;

import org.opendaylight.jsonrpc.bus.api.MessageListener;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public abstract class AbstractMessageListenerAdapter<T> extends SimpleChannelInboundHandler<T> {
    protected final MessageListener messageListener;

    protected AbstractMessageListenerAdapter(MessageListener messageListener) {
        this.messageListener = Objects.requireNonNull(messageListener);
    }
}
