/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import org.opendaylight.mdsal.dom.api.DOMRpcException;

/**
 * Simplistic implementation of {@link DOMRpcException}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public class RpcExceptionImpl extends DOMRpcException {
    private static final long serialVersionUID = -6985208519310575311L;

    public RpcExceptionImpl(final String message) {
        super(message, null);
    }

    public RpcExceptionImpl(final String message, final Throwable cause) {
        super(message, cause);
    }
}
