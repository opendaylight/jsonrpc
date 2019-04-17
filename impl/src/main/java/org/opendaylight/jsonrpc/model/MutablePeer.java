/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataConfigEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.DataOperationalEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.NotificationEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.peer.RpcEndpoints;

public class MutablePeer implements Peer {
    private String name;
    private final List<RpcEndpoints> rpcEndpoints = new ArrayList<>();
    private final List<DataOperationalEndpoints> dataOperationalEndpoints = new ArrayList<>();
    private final List<DataConfigEndpoints> endpoints = new ArrayList<>();
    private final List<NotificationEndpoints> notificationEndpoints = new ArrayList<>();
    private final List<YangIdentifier> models = new ArrayList<>();

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
    public List<YangIdentifier> getModules() {
        return models;
    }

    @Override
    public List<DataConfigEndpoints> getDataConfigEndpoints() {
        return endpoints;
    }

    @Override
    public List<DataOperationalEndpoints> getDataOperationalEndpoints() {
        return dataOperationalEndpoints;
    }

    @Override
    public List<RpcEndpoints> getRpcEndpoints() {
        return rpcEndpoints;
    }

    @Override
    public List<NotificationEndpoints> getNotificationEndpoints() {
        return notificationEndpoints;
    }
}
