/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ActualEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.config.ConfiguredEndpointsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyStep;
import org.opendaylight.yangtools.yang.common.Uint16;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Cluster related helper methods.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 1, 2020
 */
final class ClusterUtil {
    static final FiniteDuration DEFAULT_WRITE_TX_TIMEOUT = Duration.apply(120, TimeUnit.SECONDS);
    static final FiniteDuration DEFAULT_ASK_TIMEOUT = Duration.apply(10, TimeUnit.SECONDS);
    static final FiniteDuration DEFAULT_RPC_TIMEOUT = Duration.apply(30, TimeUnit.SECONDS);

    private ClusterUtil() {
        // utility class
    }

    /**
     * Get {@link DataTreeIdentifier} corresponding to {@link Peer}'s operational state.
     *
     * @param name name of peer
     * @return {@link DataTreeIdentifier}
     */
    public static DataTreeIdentifier<ActualEndpoints> getPeerOpstateIdentifier(String name) {
        return DataTreeIdentifier.of(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(Config.class)
                        .child(ActualEndpoints.class, new ActualEndpointsKey(name))
                        .build());
    }

    /**
     * Get {@link DataTreeIdentifier} corresponding to list of {@link ConfiguredEndpoints} in config DS.
     *
     * @return {@link DataTreeIdentifier}
     */
    public static DataTreeIdentifier<ConfiguredEndpoints> getPeerListIdentifier() {
        return DataTreeIdentifier.of(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(Config.class).child(ConfiguredEndpoints.class).build());
    }

    /**
     * Extract {@link Peer}'s name from {@link Peer}'s {@link InstanceIdentifier}.
     *
     * @param ii {@link InstanceIdentifier} of subtype of {@link Peer}
     * @return peer's name
     */
    public static String peerNameFromII(InstanceIdentifier<? extends Peer> ii) {
        final var last = Iterables.getLast(ii.getPathArguments());
        Preconditions.checkArgument(last instanceof KeyStep);
        if (((KeyStep<?, ?>) last).key() instanceof ConfiguredEndpointsKey) {
            return ((ConfiguredEndpointsKey) ((KeyStep<?, ?>) last).key()).getName();
        }
        if (((KeyStep<?, ?>) last).key() instanceof ActualEndpointsKey) {
            return ((ActualEndpointsKey) ((KeyStep<?, ?>) last).key()).getName();
        }
        throw new IllegalArgumentException("Unrecognized key : " + last);
    }

    public static String createActorPath(final String masterAddress, final String name) {
        return String.format("%s/user/%s", masterAddress, name);
    }

    public static String createMasterActorName(final String name, final String masterAddress) {
        return String.format("%s_%s", masterAddress.replaceAll("//", ""), name);
    }

    public static Duration durationFromUint16seconds(Uint16 timeout, FiniteDuration defValue) {
        if (timeout == null) {
            return defValue;
        }
        return Duration.apply(timeout.doubleValue(), TimeUnit.SECONDS);
    }
}
