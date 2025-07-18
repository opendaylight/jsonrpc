/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.messages;

import java.io.Serializable;
import org.apache.pekko.actor.ActorRef;

/**
 * Sent by slave to initiate slave mountpoint creation.
 */
public class MountPointRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ActorRef slaveActorRef;

    public MountPointRequest(ActorRef slaveActorRef) {
        this.slaveActorRef = slaveActorRef;
    }

    public ActorRef getSlaveActorRef() {
        return slaveActorRef;
    }

    @Override
    public String toString() {
        return "MountPointRequest [slaveActorRef=" + slaveActorRef + "]";
    }
}