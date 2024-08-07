/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataConfigEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataConfigEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataOperationalEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataOperationalEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.NotificationEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.NotificationEndpointsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpointsKey;
import org.opendaylight.yangtools.binding.util.BindingMap;

public class MutablePeer implements Peer {
    private String name;
    private final List<RpcEndpoints> rpcEndpoints = new ArrayList<>();
    private final List<DataOperationalEndpoints> dataOperationalEndpoints = new ArrayList<>();
    private final List<DataConfigEndpoints> endpoints = new ArrayList<>();
    private final List<NotificationEndpoints> notificationEndpoints = new ArrayList<>();
    private final Set<YangIdentifier> models = new HashSet<>();

    // Builder-friendly methods
    public MutablePeer name(String newName) {
        this.name = newName;
        return this;
    }

    public MutablePeer addRpcEndpoint(RpcEndpoints endpoint) {
        this.rpcEndpoints.add(endpoint);
        return this;
    }

    public MutablePeer addModels(List<YangIdentifier> yangIds) {
        this.models.addAll(yangIds);
        return this;
    }

    public MutablePeer addDataOperationalEndpoint(DataOperationalEndpoints endpoint) {
        this.dataOperationalEndpoints.add(endpoint);
        return this;
    }

    public MutablePeer addDataConfigEndpoint(DataConfigEndpoints endpoint) {
        this.endpoints.add(endpoint);
        return this;
    }

    public MutablePeer addNotificationEndpoint(NotificationEndpoints endpoint) {
        this.notificationEndpoints.add(endpoint);
        return this;
    }

    @Override
    public Class<? extends Peer> implementedInterface() {
        return MutablePeer.class;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<YangIdentifier> getModules() {
        return models;
    }

    @Override
    public Map<DataConfigEndpointsKey, DataConfigEndpoints> getDataConfigEndpoints() {
        return BindingMap.ordered(endpoints);
    }

    @Override
    public Map<DataOperationalEndpointsKey, DataOperationalEndpoints> getDataOperationalEndpoints() {
        return BindingMap.ordered(dataOperationalEndpoints);
    }

    @Override
    public Map<RpcEndpointsKey, RpcEndpoints> getRpcEndpoints() {
        return BindingMap.ordered(rpcEndpoints);
    }

    @Override
    public Map<NotificationEndpointsKey, NotificationEndpoints> getNotificationEndpoints() {
        return BindingMap.ordered(notificationEndpoints);
    }
}
