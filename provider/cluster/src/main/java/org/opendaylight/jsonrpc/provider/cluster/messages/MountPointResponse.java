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
 * Sent by master to slave in response to {@link MountPointRequest}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 11, 2020
 */
public class MountPointResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private ActorRef masterActorRef;

    public MountPointResponse(ActorRef masterActorRef) {
        this.masterActorRef = masterActorRef;
    }

    public ActorRef getMasterActorRef() {
        return masterActorRef;
    }

    @Override
    public String toString() {
        return "MountPointResponse [masterActorRef=" + masterActorRef + "]";
    }
}
