/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.KeyStep;
import org.opendaylight.yangtools.yang.common.Uint16;
import scala.concurrent.duration.FiniteDuration;

/**
 * Cluster related helper methods.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 1, 2020
 */
final class ClusterUtil {
    static final FiniteDuration DEFAULT_WRITE_TX_TIMEOUT = FiniteDuration.create(120, TimeUnit.SECONDS);
    static final FiniteDuration DEFAULT_ASK_TIMEOUT = FiniteDuration.create(10, TimeUnit.SECONDS);
    static final FiniteDuration DEFAULT_RPC_TIMEOUT = FiniteDuration.create(30, TimeUnit.SECONDS);

    private ClusterUtil() {
        // utility class
    }

    /**
     * Get {@link DataTreeIdentifier} corresponding to {@link Peer}'s operational state.
     *
     * @param name name of peer
     * @return {@link DataTreeIdentifier}
     */
    static DataTreeIdentifier<ActualEndpoints> getPeerOpstateIdentifier(final String name) {
        return DataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(Config.class)
                        .child(ActualEndpoints.class, new ActualEndpointsKey(name))
                        .build());
    }

    /**
     * Extract {@link Peer}'s name from {@link Peer}'s {@link InstanceIdentifier}.
     *
     * @param ii {@link InstanceIdentifier} of subtype of {@link Peer}
     * @return peer's name
     */
    static String peerNameFromII(final DataObjectIdentifier<? extends Peer> ii) {
        final var last = ii.lastStep();
        if (!(last instanceof KeyStep<?, ?> keyStep)) {
            throw new IllegalArgumentException("Unexpected last step " + last);
        }
        return switch (keyStep.key()) {
            case ConfiguredEndpointsKey key -> key.getName();
            case ActualEndpointsKey key -> key.getName();
            default -> throw new IllegalArgumentException("Unrecognized key : " + keyStep);
        };
    }

    static String createActorPath(final String masterAddress, final String name) {
        return "%s/user/%s".formatted(masterAddress, name);
    }

    static String createMasterActorName(final String name, final String masterAddress) {
        return "%s_%s".formatted(masterAddress.replace("//", ""), name);
    }

    static FiniteDuration durationFromUint16seconds(final Uint16 timeout, final FiniteDuration defValue) {
        return timeout == null ? defValue : FiniteDuration.create(timeout.toJava(), TimeUnit.SECONDS);
    }
}
