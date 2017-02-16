/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationPublishServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationServiceAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.controller.md.sal.dom.broker.impl.SerializedDOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.mount.DOMMountPointServiceImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.mdsal.binding.dom.adapter.test.util.MockSchemaService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import javassist.ClassPool;

/**
 * Enhanced version of
 * {@link org.opendaylight.mdsal.binding.dom.adapter.test.DataBrokerTestCustomizer}
 * 
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public class DataBrokerTestCustomizer {
    private final ExecutorService exec = Executors.newCachedThreadPool();
    private DOMDataBroker domDataBroker;
    private final DOMNotificationRouter domNotificationRouter;
    private final DOMMountPointService domMountPointService;
    private final MockSchemaService schemaService;
    private ImmutableMap<LogicalDatastoreType, DOMStore> datastores;
    private final BindingToNormalizedNodeCodec bindingToNormalized;

    public ImmutableMap<LogicalDatastoreType, DOMStore> createDatastores() {
        return ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, createOperationalDatastore())
                .put(LogicalDatastoreType.CONFIGURATION, createConfigurationDatastore()).build();
    }

    public DataBrokerTestCustomizer() {
        schemaService = new MockSchemaService();
        final ClassPool pool = ClassPool.getDefault();
        final DataObjectSerializerGenerator generator = StreamWriterGenerator.create(JavassistUtils.forClassPool(pool));
        final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(generator);
        final GeneratedClassLoadingStrategy loading = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();
        bindingToNormalized = new BindingToNormalizedNodeCodec(loading, codecRegistry);
        schemaService.registerSchemaContextListener(bindingToNormalized);
        domNotificationRouter = DOMNotificationRouter.create(16);
        domMountPointService = new DOMMountPointServiceImpl();
    }

    public DOMStore createConfigurationDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("CFG", exec);
        schemaService.registerSchemaContextListener(store);
        return store;
    }

    public DOMStore createOperationalDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", exec);
        schemaService.registerSchemaContextListener(store);
        return store;
    }

    public DOMDataBroker createDOMDataBroker() {
        return new SerializedDOMDataBroker(getDatastores(), getCommitCoordinatorExecutor());
    }

    public NotificationService createNotificationService() {
        return new BindingDOMNotificationServiceAdapter(bindingToNormalized.getCodecRegistry(), domNotificationRouter);
    }

    public NotificationPublishService createNotificationPublishService() {
        return new BindingDOMNotificationPublishServiceAdapter(bindingToNormalized, domNotificationRouter);
    }

    public ListeningExecutorService getCommitCoordinatorExecutor() {
        return MoreExecutors.newDirectExecutorService();
    }

    public DataBroker createDataBroker() {
        return new BindingDOMDataBrokerAdapter(getDOMDataBroker(), bindingToNormalized);
    }

    public BindingToNormalizedNodeCodec getBindingToNormalized() {
        return bindingToNormalized;
    }

    public DOMSchemaService getSchemaService() {
        return schemaService;
    }

    private DOMDataBroker getDOMDataBroker() {
        if (domDataBroker == null) {
            domDataBroker = createDOMDataBroker();
        }
        return domDataBroker;
    }

    private synchronized ImmutableMap<LogicalDatastoreType, DOMStore> getDatastores() {
        if (datastores == null) {
            datastores = createDatastores();
        }
        return datastores;
    }

    public void updateSchema(final SchemaContext ctx) {
        schemaService.changeSchema(ctx);
    }

    public DOMNotificationRouter getDomNotificationRouter() {
        return domNotificationRouter;
    }

    public DOMMountPointService getDOMMountPointService() {
        return domMountPointService;
    }
}