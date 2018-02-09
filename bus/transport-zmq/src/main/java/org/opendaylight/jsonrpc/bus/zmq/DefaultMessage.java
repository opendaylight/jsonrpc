/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.ObjectUtil;

/**
 * Default implementation of {@link Message}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 17, 2018
 */
public class DefaultMessage implements Message {
    private final ByteBuf content;
    private final boolean isLast;

    public DefaultMessage(final boolean isLast, ByteBuf content) {
        this.content = ObjectUtil.checkNotNull(content, "Content");
        this.isLast = isLast;
    }

    @Override
    public ByteBuf toBuffer() {
        return content.duplicate();
    }

    @Override
    public boolean last() {
        return isLast;
    }

    @Override
    public String toString() {
        return "DefaultMessage [isLast=" + isLast + ", content=" + content + "]";
    }
}
