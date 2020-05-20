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
import java.util.Collection;
import java.util.Objects;
import org.opendaylight.jsonrpc.model.ModuleInfo;

/**
 * Message sent by master to slave nodes to instruct them to create mount point.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jun 27, 2020
 */
public class RegisterMountPoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Collection<ModuleInfo> modules;
    private final ActorRef masterActorRef;

    public RegisterMountPoint(final Collection<ModuleInfo> modules, ActorRef masterActorRef) {
        this.modules = Objects.requireNonNull(modules);
        this.masterActorRef = Objects.requireNonNull(masterActorRef);
    }

    public Collection<ModuleInfo> getModules() {
        return modules;
    }

    public ActorRef getMasterActorRef() {
        return masterActorRef;
    }
}
