/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.spi;

import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Service Component Runtime integration.
 */
@Component(service = { }, configurationPid = "org.opendaylight.jsonrpc.bus")
@Designate(ocd = SCRIntegration.Configuration.class)
public final class SCRIntegration {
    @ObjectClassDefinition
    @interface Configuration {
        @AttributeDefinition(min = "1")
        int boss$_$group$_$size() default 2;
        @AttributeDefinition(min = "1")
        int worker$_$group$_$size() default 4;
        @AttributeDefinition(min = "1")
        int handler$_$group$_$size() default 4;
    }

    private static final String PROP_SIZE = ".size";
    private static final String PROP_THREAD_FACTORY = ".threadFactory";

    private final ThreadFactory bossThreadFactory = ThreadFactoryProvider.create("boss");
    private final ThreadFactory workerThreadFactory = ThreadFactoryProvider.create("worker");
    private final ThreadFactory handlerThreadFactory = ThreadFactoryProvider.create("handler");
    private final ComponentFactory<SCREventExecutorGroup> executorGroupFactory;
    private final ComponentFactory<SCREventLoopGroup> loopGroupFactory;

    private ComponentInstance<SCREventLoopGroup> boss;
    private ComponentInstance<SCREventLoopGroup> worker;
    private ComponentInstance<SCREventExecutorGroup> handler;
    private int bossSize;
    private int workerSize;
    private int handlerSize;

    @Activate
    public SCRIntegration(
            @Reference(target = "(component.factory=" + SCREventExecutorGroup.FACTORY_NAME + ")")
            ComponentFactory<SCREventExecutorGroup> executorGroupFactory,
            @Reference(target = "(component.factory=" + SCREventLoopGroup.FACTORY_NAME + ")")
            ComponentFactory<SCREventLoopGroup> loopGroupFactory,
            Configuration configuration) {
        this.executorGroupFactory = Objects.requireNonNull(executorGroupFactory);
        this.loopGroupFactory = Objects.requireNonNull(loopGroupFactory);

        bossSize = configuration.boss$_$group$_$size();
        workerSize = configuration.worker$_$group$_$size();
        handlerSize = configuration.handler$_$group$_$size();

        startBoss();
        startWorker();
        startHandler();
    }

    @Modified
    public void modified(Configuration configuration) {
        // Dispose old instances first so we do not expose partial view
        int newBossSize = configuration.boss$_$group$_$size();
        if (bossSize != newBossSize) {
            boss.dispose();
            boss = null;
            bossSize = newBossSize;
        }
        int newWorkerSize = configuration.worker$_$group$_$size();
        if (workerSize != newWorkerSize) {
            worker.dispose();
            worker = null;
            workerSize = newWorkerSize;
        }
        int newHandlerSize = configuration.handler$_$group$_$size();
        if (handlerSize != newHandlerSize) {
            handler.dispose();
            handler = null;
            handlerSize = newHandlerSize;
        }

        if (boss == null) {
            startBoss();
        }
        if (worker == null) {
            startWorker();
        }
        if (handler == null) {
            startHandler();
        }
    }

    @Deactivate
    public void deactivate() {
        handler.dispose();
        worker.dispose();
        boss.dispose();
    }

    private void startBoss() {
        boss = loopGroupFactory.newInstance(loopProps("boss", bossSize, bossThreadFactory));
    }

    private void startWorker() {
        worker = loopGroupFactory.newInstance(loopProps("worker", workerSize, workerThreadFactory));
    }

    private void startHandler() {
        handler = executorGroupFactory.newInstance(FrameworkUtil.asDictionary(Map.of(
            "name", "jsonrpc",
            PROP_SIZE, handlerSize,
            PROP_THREAD_FACTORY, handlerThreadFactory)));
    }

    static int size(Map<String, ?> properties) {
        return prop(properties, PROP_SIZE, Integer.class);
    }

    static ThreadFactory threadFactory(Map<String, ?> properties) {
        return prop(properties, PROP_THREAD_FACTORY, ThreadFactory.class);
    }

    private static Dictionary<String, ?> loopProps(String type, int size, ThreadFactory threadFactory) {
        return FrameworkUtil.asDictionary(Map.of(
            "name", "jsonrpc",
            "type", type,
            PROP_SIZE, size,
            PROP_THREAD_FACTORY, threadFactory));
    }

    private static <T> T prop(Map<String, ?> properties, String name, Class<T> type) {
        return type.cast(Objects.requireNonNull(properties.get(name)));
    }
}
