/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Special type of {@link Future} which combines blocking on
 * {@link #get(long, TimeUnit)} with 2 distinct futures. First is used to wait
 * for connection to be established and second is actual {@link ChannelPromise}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jun 30, 2018
 */
public class CombinedFuture<T> extends DefaultPromise<T> {
    private Future<?> connectionFuture;
    private Promise<T> resultFuture;

    public CombinedFuture(Future<?> connectionFuture, Promise<T> resultFuture) {
        this.connectionFuture = Objects.requireNonNull(connectionFuture);
        this.resultFuture = Objects.requireNonNull(resultFuture);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        connectionFuture.get();
        return resultFuture.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        final long start = System.nanoTime();
        connectionFuture.get(timeout, unit);
        // decrement by time spent in connectionFuture#get()
        long diff = System.nanoTime() - start;
        return resultFuture.get(unit.toNanos(timeout) - diff, TimeUnit.NANOSECONDS);
    }

    @Override
    public Promise<T> addListener(GenericFutureListener<? extends Future<? super T>> listener) {
        return resultFuture.addListener(listener);
    }
}
