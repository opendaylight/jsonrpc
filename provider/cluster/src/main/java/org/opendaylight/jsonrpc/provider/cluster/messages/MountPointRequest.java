/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.messages;

import akka.actor.ActorRef;
import java.io.Serializable;

/**
 * Sent by slave to initiate slave mountpoint creation.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 9, 2020
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
}