/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

/**
 * Callback to perform cleanup of given {@link AutoCloseable}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 20, 2018
 */
public interface CloseCallback {
    /**
     * Called when {@link AutoCloseable} is about to be closed.
     *
     * @param closeable {@link AutoCloseable} instance to clean up.
     */
    void onClose(AutoCloseable closeable);
}
