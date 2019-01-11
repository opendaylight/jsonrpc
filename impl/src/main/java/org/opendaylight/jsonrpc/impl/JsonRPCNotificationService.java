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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    public JsonRPCNotificationService(@Nonnull Peer peer, @Nonnull SchemaContext schemaContext,
            @Nonnull HierarchicalEnumMap<JsonElement, DataType, String> pathMap, @Nonnull JsonConverter jsonConverter,
            @Nonnull TransportFactory transportFactory, @Nullable RemoteGovernance governance)
            throws URISyntaxException {
        super(schemaContext, transportFactory, pathMap, jsonConverter, peer);

        if (peer.getNotificationEndpoints() != null) {
            Util.populateFromEndpointList(pathMap, peer.getNotificationEndpoints(), DataType.NOTIFICATION);
        }

        for (final NotificationDefinition def : schemaContext.getNotifications()) {
            final QNameModule qm = def.getQName().getModule();
            final Optional<Module> possibleModule = schemaContext.findModule(qm.getNamespace(), qm.getRevision());
            if (!possibleModule.isPresent()) {
                LOG.error("Notification {} cannot be mapped, module not found", def.getQName().getLocalName());
                continue;
            }
            final String topLevel = jsonConverter.makeQualifiedName(possibleModule.get(), def.getQName());
            final JsonObject path = new JsonObject();
            path.add(topLevel, new JsonObject());
            final String notificationEndpoint = getEndpoint(peer, pathMap, governance, path);
            if (notificationEndpoint != null) {
                LOG.info("Notification {} mapped to {}", topLevel, notificationEndpoint);
                mappedNotifications.put(def.getQName().getLocalName(),
                        new NotificationState(def, notificationEndpoint, this, transportFactory));
            } else {
                LOG.error("Notifications {} cannot be mapped, no known endpoint", topLevel);
            }
        }
    }

    private String getEndpoint(Peer peer, HierarchicalEnumMap<JsonElement, DataType, String> pathMap,
            RemoteGovernance governance, final JsonObject path) {
        String notificationEndpoint = pathMap.lookup(path, DataType.NOTIFICATION).orElse(null);
        LOG.debug("Notification endpoint - map lookup is  {}", notificationEndpoint);
        if (notificationEndpoint == null && governance != null) {
            notificationEndpoint = governance.governance(-1, peer.getName(), path);
            if (notificationEndpoint != null) {
                /*
                 * Store the endpoint from governance so we can generate actual
                 * opstate
                 */
                pathMap.put(path, DataType.NOTIFICATION, notificationEndpoint);
            }
        }
        return notificationEndpoint;
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
            @Nonnull final T listener, @Nonnull final Collection<SchemaPath> types) {
        for (final SchemaPath type : types) {
            listeners.put(type, listener);
        }
        return new AbstractListenerRegistration<T>(listener) {
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
            @Nonnull final T listener, final SchemaPath... types) {
        return registerNotificationListener(listener, Lists.newArrayList(types));
    }

    @Override
    public void handleNotification(JsonRpcNotificationMessage notification) {
        // Publish notification
        publishNotification(jsonConverter.notificationConvert(notification, mappedNotifications));
    }
}
