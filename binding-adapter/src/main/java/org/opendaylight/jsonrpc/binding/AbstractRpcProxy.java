/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.binding;

import org.opendaylight.yangtools.concepts.AbstractRegistration;

/**
 * A common superclass for {@link SingleRpcProxy} and {@link MultiRpcProxy}.
 */
public abstract sealed class AbstractRpcProxy extends AbstractRegistration permits SingleRpcProxy, MultiRpcProxy {
    /**
     * Check if the underlying connect is ready.
     *
     * @return {@code true} if the connection is ready, {@code false} otherwise
     */
    public abstract boolean isConnectionReady();
}
