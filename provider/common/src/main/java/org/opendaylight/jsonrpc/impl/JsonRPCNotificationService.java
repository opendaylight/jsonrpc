/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.opendaylight.jsonrpc.provider.common.Util.findNode;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcNotificationMessage;
import org.opendaylight.jsonrpc.bus.messagelib.NotificationMessageHandler;
import org.opendaylight.jsonrpc.bus.messagelib.SubscriberSession;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.mdsal.dom.api.DOMNotificationListener;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonRPCNotificationService extends AbstractJsonRPCComponent
        implements DOMNotificationService, NotificationMessageHandler, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCNotificationService.class);
    private final Multimap<Absolute, DOMNotificationListener> listeners = HashMultimap.create();
    private final Map<QName, SubscriberSession> mapped;

    public JsonRPCNotificationService(final @NonNull Peer peer, final @NonNull EffectiveModelContext schemaContext,
            final @NonNull HierarchicalEnumMap<JsonElement, DataType, String> pathMap,
            final @NonNull JsonRpcCodecFactory codecFactory, final @NonNull TransportFactory transportFactory,
            final @Nullable RemoteGovernance governance) throws URISyntaxException {
        super(schemaContext, transportFactory, pathMap, codecFactory, peer);

        final ImmutableMap.Builder<QName, SubscriberSession> builder = ImmutableMap.builder();

        for (final NotificationDefinition def : schemaContext.getNotifications()) {
            final QNameModule qm = def.getQName().getModule();
            final String localName = def.getQName().getLocalName();
            final Optional<Module> possibleModule = schemaContext.findModule(qm);
            final JsonObject path = createRootPath(possibleModule.orElseThrow(), def.getQName());
            final String endpoint = getEndpoint(DataType.NOTIFICATION, governance, path);
            if (endpoint != null) {
                LOG.debug("Notification '{}' mapped to {}", localName, endpoint);
                builder.put(def.getQName(), transportFactory.endpointBuilder().subscriber().create(endpoint, this));
            } else {
                LOG.debug("Notification '{}' cannot be mapped, no known endpoint", localName);
            }
        }
        mapped = builder.build();
    }

    @Override
    public void close() {
        // Close all notification listeners
        mapped.values().forEach(SubscriberSession::close);
        listeners.clear();
    }

    /*
     * Notification listener is running in a separate thread so invoking them from here is not an issue (this is the
     * major difference between this code and netconf - it was nearly verbatim lifted out of there
     */
    private synchronized void publishNotification(final DOMNotification notification) {
        listeners.get(notification.getType()).forEach(l -> {
            LOG.debug("Invoking listener {} with notification {}", l, notification);
            l.onNotification(notification);
        });
    }

    @Override
    public synchronized Registration registerNotificationListener(final DOMNotificationListener listener,
            final Collection<Absolute> types) {
        for (final Absolute type : types) {
            listeners.put(type, listener);
        }
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                for (final Absolute type : types) {
                    listeners.remove(type, listener);
                }
            }
        };
    }

    @Override
    public Registration registerNotificationListeners(final Map<Absolute, DOMNotificationListener> typeToListener) {
        for (final Entry<Absolute, DOMNotificationListener> entry : typeToListener.entrySet()) {
            listeners.put(entry.getKey(), entry.getValue());
        }
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                for (final Entry<Absolute, DOMNotificationListener> entry : typeToListener.entrySet()) {
                    listeners.remove(entry.getKey(), entry.getValue());
                }
            }
        };
    }

    @Override
    public void handleNotification(final JsonRpcNotificationMessage notification) {
        try {
            final NotificationDefinition def = findNode(schemaContext, notification.getMethod(),
                    Module::getNotifications)
                            .orElseThrow(() -> new IllegalStateException(
                                    String.format("Notification with name '%s' not found", notification.getMethod())));
            publishNotification(codecFactory.notificationCodec(def).deserialize(notification.getParams()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
