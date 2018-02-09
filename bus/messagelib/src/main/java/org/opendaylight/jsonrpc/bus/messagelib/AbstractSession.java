/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractSession implements BaseSession {
    private final AtomicInteger id = new AtomicInteger();
    private AutoCloseable closeable;
    private final CloseCallback closeCallback;
    protected long timeout;

    AbstractSession(CloseCallback closeCallback) {
        this.closeCallback = Objects.requireNonNull(closeCallback);
        this.timeout = 30_000L;
    }

    protected void setAutocloseable(AutoCloseable autoCloseable) {
        this.closeable = autoCloseable;
    }

    protected int nextId() {
        return id.incrementAndGet();
    }

    @Override
    public void setTimeout(long timeoutMilliseconds) {
        this.timeout = timeoutMilliseconds;
    }

    @Override
    public void close() {
        closeCallback.onClose(closeable);
        Util.closeQuietly(closeable);
    }

    @Override
    public String toString() {
        return "AbstractSession [closeable=" + closeable + ", timeout=" + timeout + "]";
    }
}
