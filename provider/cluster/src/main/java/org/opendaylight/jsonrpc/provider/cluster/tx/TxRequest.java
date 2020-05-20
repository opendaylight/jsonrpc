/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.tx;

import java.io.Serializable;

/**
 * Send by slave mountpoint to request new DOM transaction. In JSONRPC implementation, there is no distinction between
 * RO/RW/WO transaction types.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 14, 2020
 */
public class TxRequest implements Serializable {
    private static final long serialVersionUID = 1L;
}
