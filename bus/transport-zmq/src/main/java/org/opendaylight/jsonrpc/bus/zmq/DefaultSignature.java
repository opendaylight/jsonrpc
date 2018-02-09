/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

/**
 * Default implementation of {@link Signature}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Feb 16, 2018
 */
public class DefaultSignature implements Signature {
    private final long padding;

    public DefaultSignature(long padding) {
        this.padding = padding;
    }

    public DefaultSignature() {
        this(1);
    }

    @Override
    public long padding() {
        return padding;
    }

    @Override
    public String toString() {
        return "DefaultSignature [padding=" + padding + "]";
    }
}
