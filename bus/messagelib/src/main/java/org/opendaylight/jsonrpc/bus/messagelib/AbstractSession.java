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
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSession implements BaseSession {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSession.class);
    private final AtomicInteger id = new AtomicInteger();
    private AutoCloseable closeable;
    private final Consumer<AutoCloseable> closeCallback;
    private final AtomicInteger refCount = new AtomicInteger(0);
    protected final long timeout;

    AbstractSession(Consumer<AutoCloseable> closeCallback, String uri) {
        this.closeCallback = Objects.requireNonNull(closeCallback);
        this.timeout = Util.timeoutFromUri(uri);
    }

    protected void setAutocloseable(AutoCloseable autoCloseable) {
        this.closeable = autoCloseable;
    }

    protected int nextId() {
        return id.incrementAndGet();
    }

    @Override
    public void close() {
        if (refCount.decrementAndGet() == 0) {
            LOG.debug("Reference count reached 0, closing and removing {}", closeable);
            Util.closeQuietly(closeable);
            closeCallback.accept(this);
        }
    }

    public void addReference() {
        refCount.incrementAndGet();
    }

    @Override
    public String toString() {
        return "AbstractSession [closeable=" + closeable + ", timeout=" + timeout + "]";
    }
}
