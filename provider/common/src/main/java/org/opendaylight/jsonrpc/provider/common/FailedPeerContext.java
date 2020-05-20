/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import static com.google.common.base.Throwables.getRootCause;

import com.google.common.annotations.Beta;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.MountStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsBuilder;

/**
 * Context of {@link Peer} that failed mount operation. This class is necessary to keep operational state and internal
 * state in-sync.
 *
 * @see MappedPeerContext
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Mar 11, 2020
 */
@Beta
public final class FailedPeerContext extends AbstractPeerContext {
    private final String cause;

    public FailedPeerContext(Peer peer, DataBroker dataBroker, Exception cause) {
        super(peer, dataBroker);
        this.cause = cause.getMessage();
        publishState(new ActualEndpointsBuilder(peer), MountStatus.Failed, Optional.of(getRootCause(cause)));
    }

    @Override
    public String toString() {
        return "FailedPeerContext [peer=" + peer + ", cause=" + cause + "]";
    }
}
