/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.hmap;

import java.util.Objects;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import com.google.common.collect.ImmutableMap;

/**
 * Used to differentiate between data keys in {@link HierarchicalEnumMap}
 * 
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public enum DataType {
    /**
     * Operational data
     */
    OPERATIONAL_DATA,
    /**
     * Configuration data
     */
    CONFIGURATION_DATA,
    /**
     * RPCs
     */
    RPC,
    /**
     * Notifications
     */
    NOTIFICATION;

    private static final ImmutableMap<LogicalDatastoreType, DataType> DS_MAP = ImmutableMap
            .<LogicalDatastoreType, DataType>builder()
            .put(LogicalDatastoreType.CONFIGURATION, DataType.CONFIGURATION_DATA)
            .put(LogicalDatastoreType.OPERATIONAL, DataType.OPERATIONAL_DATA).build();

    public static DataType forDatastore(LogicalDatastoreType ldt) {
        return Objects.requireNonNull(DS_MAP.get(ldt), "Can't map data type into datastore using type " + ldt);
    }
}
