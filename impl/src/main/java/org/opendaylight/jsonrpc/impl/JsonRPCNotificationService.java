/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcException;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcNotificationMessage;
import org.opendaylight.jsonrpc.bus.messagelib.NotificationMessageHandler;
import org.opendaylight.jsonrpc.bus.messagelib.TransportFactory;
import org.opendaylight.jsonrpc.hmap.DataType;
import org.opendaylight.jsonrpc.hmap.HierarchicalEnumMap;
import org.opendaylight.jsonrpc.model.JsonRpcNotification;
import org.opendaylight.jsonrpc.model.NotificationContainerProxy;
import org.opendaylight.jsonrpc.model.NotificationState;
import org.opendaylight.jsonrpc.model.RemoteGovernance;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRPCNotificationService implements DOMNotificationService, NotificationMessageHandler, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(JsonRPCNotificationService.class);
    private final Multimap<SchemaPath, DOMNotificationListener> listeners = HashMultimap.create();
    private final SchemaContext schemaContext;
    private final Map<String, NotificationState> mappedNotifications = new HashMap<>();

    public JsonRPCNotificationService(@Nonnull Peer peer, @Nonnull SchemaContext schemaContext,
            @Nonnull HierarchicalEnumMap<JsonElement, DataType, String> pathMap,
            @Nonnull TransportFactory transportFactory, @Nullable RemoteGovernance governance)
            throws URISyntaxException {
        this.schemaContext = Preconditions.checkNotNull(schemaContext);
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(transportFactory);
        Preconditions.checkNotNull(pathMap);

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
            final StringBuilder sb = new StringBuilder();
            sb.append(possibleModule.get().getName());
            sb.append(':');
            sb.append(def.getQName().getLocalName());
            final String topLevel = sb.toString();
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
        ListenableFuture<List<Void>> futures = Futures.allAsList(mappedNotifications.values().stream()
                .map(ns -> ns.client().stop()).collect(Collectors.toList()));
        try {
            futures.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while stopping notification listener sessions", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Failed to stop some/all notification listener sessions", e);
        }

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

        return new ListenerRegistration<T>() {
            @Override
            public void close() {
                for (final SchemaPath type : types) {
                    listeners.remove(type, listener);
                }
            }

            @Override
            public T getInstance() {
                return listener;
            }
        };
    }

    @Override
    public synchronized <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(
            @Nonnull final T listener, final SchemaPath... types) {
        return registerNotificationListener(listener, Lists.newArrayList(types));
    }

    private DOMNotification extractNotification(NotificationState notificationState, JsonElement jsonResult,
            DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> notificationBuilder, Date eventTime) {

        try (NormalizedNodeStreamWriter streamWriter = Util
                .wrapWithAnyXmlNullValueCallBack(ImmutableNormalizedNodeStreamWriter.from(notificationBuilder))) {
            try (JsonParserStream jsonParser = JsonParserStream.create(streamWriter, schemaContext,
                    new NotificationContainerProxy(notificationState.notification()))) {
                jsonParser.parse(new JsonReader(new StringReader(jsonResult.toString())));
                return new JsonRpcNotification(notificationBuilder.build(), eventTime,
                        notificationState.notification().getPath());
            } catch (IOException e) {
                LOG.error("Failed to process JSON", e);
                return null;
            }
        } catch (IOException e1) {
            LOG.error("Failed to close JSON parser", e1);
            return null;
        }
    }

    @Override
    public void handleNotification(JsonRpcNotificationMessage notification) {
        String method = notification.getMethod();
        JsonElement parsed;
        NotificationState ns;

        LOG.debug("Got notification {}", notification);
        try {
            parsed = notification.getParamsAsObject(JsonElement.class);
            ns = Preconditions.checkNotNull(mappedNotifications.get(method));
        } catch (JsonRpcException e) {
            LOG.error("Error processing notification", e);
            return;
        } catch (IllegalStateException e) {
            LOG.error("Notification not mapped {}", method, e);
            return;
        }

        JsonObject digested;
        if (parsed.isJsonObject()) {
            digested = parsed.getAsJsonObject();
        } else {
            digested = new JsonObject();
            int count = 0;
            for (DataSchemaNode child : ns.notification().getChildNodes()) {
                try {
                    digested.add(child.getQName().getLocalName(), parsed.getAsJsonArray().get(count));
                } catch (IndexOutOfBoundsException e) {
                    // Do nothing - leave that element empty - we got a null
                    LOG.debug("Failed to process element as array : {}", parsed, e);
                }
                count++;
            }
        }

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> notificationBuilder =
                ImmutableContainerNodeBuilder.create().withNodeIdentifier(NodeIdentifier.create(
                        ns.notification().getQName()));

        Date eventTime = new Date();
        final DOMNotification deserialized = extractNotification(ns, digested, notificationBuilder, eventTime);

        LOG.debug("Deserialized {}", deserialized);

        // Publish notification
        publishNotification(deserialized);
    }
}
