/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.rpc;

import java.io.Serializable;

/**
 * Message sent when RPC invocation result is empty.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 13, 2020
 */
public class EmptyRpcResponse implements Serializable {
    private static final long serialVersionUID = 1L;
}
