/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.messages;

import java.io.Serializable;
import org.opendaylight.jsonrpc.provider.cluster.impl.JsonRpcPeerManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;

/**
 * Sent from {@link JsonRpcPeerManager} when {@link Peer}'s operational state was removed.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 1, 2020
 */
public class UnregisterMountPoint implements Serializable {
    public static final UnregisterMountPoint INSTANCE = new UnregisterMountPoint();
    private static final long serialVersionUID = 1L;
}
