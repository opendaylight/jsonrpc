/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import io.netty.util.concurrent.FastThreadLocal;

import org.opendaylight.jsonrpc.bus.api.MessageListener;
import org.opendaylight.jsonrpc.bus.api.PeerContext;

/**
 * This class acts as holder of {@link PeerContext} in thread-local storage.
 * Useful for accessing {@link PeerContext} in message handlers. General usage
 * is that you construct try-finally block within
 * {@link MessageListener#onMessage(PeerContext, String)} and call
 * {@link #set(PeerContext)} in <strong>try</strong> block and {@link #remove()}
 * in <strong>finally</strong> block. Classes that are interested in
 * {@link PeerContext} value can call {@link #get()} anywhere.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since May 15, 2018
 */
public final class PeerContextHolder {
    private static final FastThreadLocal<PeerContext> THREAD_LOCAL = new FastThreadLocal<>();

    private PeerContextHolder() {
        // no instantiation of this class
    }

    /**
     * Attach {@link PeerContext} to current thread-local storage.
     *
     * @param context {@link PeerContext} to attach to current thread
     */
    public static void set(PeerContext context) {
        THREAD_LOCAL.set(context);
    }

    /**
     * Get previously attached {@link PeerContext} from current thread-local
     * storage.
     *
     * @return {@link PeerContext} attached to current thread or null if there
     *         is no {@link PeerContext} attached
     */
    public static PeerContext get() {
        return THREAD_LOCAL.get();
    }

    /**
     * Remove previously attached {@link PeerContext} (if any) from current
     * thread's local storage.
     */
    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
