/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcNotificationMessage;
import org.opendaylight.jsonrpc.bus.messagelib.NotificationMessageHandler;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.NotificationState;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.mdsal.dom.api.DOMNotificationListener;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRPCNotificationService extends AbstractJsonRPCComponent
        implements DOMNotificationService, NotificationMessageHandler, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCNotificationService.class);
    private final Multimap<SchemaPath, DOMNotificationListener> listeners = HashMultimap.create();
    private final Map<String, NotificationState> mappedNotifications = new HashMap<>();

    public JsonRPCNotificationService(@NonNull Peer peer, @NonNull SchemaContext schemaContext,
            @NonNull HierarchicalEnumMap<JsonElement, DataType, String> pathMap, @NonNull JsonConverter jsonConverter,
            @NonNull TransportFactory transportFactory, @Nullable RemoteGovernance governance)
            throws URISyntaxException {
        super(schemaContext, transportFactory, pathMap, jsonConverter, peer);
        Util.populateFromEndpointList(pathMap, peer.getNotificationEndpoints(), DataType.NOTIFICATION);
        for (final NotificationDefinition def : schemaContext.getNotifications()) {
            final QNameModule qm = def.getQName().getModule();
            final String localName = def.getQName().getLocalName();
            final Optional<Module> possibleModule = schemaContext.findModule(qm.getNamespace(), qm.getRevision());
            final JsonObject path = createRootPath(possibleModule.get(), def.getQName());
            final String endpoint = getEndpoint(DataType.NOTIFICATION, governance, path);
            if (endpoint != null) {
                LOG.info("Notification '{}' mapped to {}", localName, endpoint);
                mappedNotifications.put(localName, new NotificationState(def, endpoint, this, transportFactory));
            } else {
                LOG.warn("Notification '{}' cannot be mapped, no known endpoint", localName);
            }
        }
    }

    @Override
    public void close() {
        // Close all notification listeners
        mappedNotifications.values().stream().forEach(NotificationState::close);
        mappedNotifications.clear();
        listeners.clear();
    }

    /*
     * Notification listener is running in a separate thread so invoking them
     * from here is not an issue (this is the major difference between this code
     * and netconf - it was nearly verbatim lifted out of there
     */
    public synchronized void publishNotification(final DOMNotification notification) {
        listeners.get(notification.getType()).forEach(l -> {
            LOG.debug("Invoking listener {} with notification {}", l, notification);
            l.onNotification(notification);
        });
    }

    @Override
    public synchronized <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(
            @NonNull final T listener, @NonNull final Collection<SchemaPath> types) {
        for (final SchemaPath type : types) {
            listeners.put(type, listener);
        }
        return new AbstractListenerRegistration<>(listener) {
            @Override
            protected void removeRegistration() {
                for (final SchemaPath type : types) {
                    listeners.remove(type, listener);
                }
            }
        };
    }

    @Override
    public synchronized <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(
            @NonNull final T listener, final SchemaPath... types) {
        return registerNotificationListener(listener, Lists.newArrayList(types));
    }

    @Override
    public void handleNotification(JsonRpcNotificationMessage notification) {
        // Publish notification
        publishNotification(jsonConverter.notificationConvert(notification, mappedNotifications));
    }
}
