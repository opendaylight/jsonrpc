/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.common;

import com.google.common.base.Strings;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import junit.framework.AssertionFailedError;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.opendaylight.jsonrpc.dom.codec.JsonRpcCodecFactory;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractDataBrokerTest;
import org.opendaylight.mdsal.binding.dom.adapter.test.ConcurrentDataBrokerTestCustomizer;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.DOMMountPointServiceImpl;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.mdsal.dom.broker.DOMRpcRouter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.data.rev201014.TopContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.notif.rev201014.Notification1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.rpc.rev201014.FactorialInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.binding.data.codec.impl.di.DefaultBindingDOMCodecFactory;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.yangtools.binding.runtime.spi.BindingRuntimeHelpers;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common test infrastructure for {@link SchemaContext} aware tests.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public abstract class AbstractJsonRpcTest extends AbstractDataBrokerTest {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractJsonRpcTest.class);
    private final DOMMountPointService domMountPointService = new DOMMountPointServiceImpl();
    private final DOMNotificationRouter notificationRouter = new DOMNotificationRouter(4);
    private DataBroker dataBroker;
    private DOMDataBroker domBroker;
    private DOMRpcRouter rpcRouter;
    protected ConcurrentDataBrokerTestCustomizer testCustomizer;
    protected EffectiveModelContext schemaContext;
    protected JsonRpcCodecFactory codecFactory;
    @Rule
    public TestName nameRule = new TestName();
    private BindingNormalizedNodeSerializer bnnc;
    protected @NonNull BindingRuntimeContext runtimeContext = BindingRuntimeHelpers.createRuntimeContext();

    @Override
    protected void setupWithSchema(final EffectiveModelContext context) {
        this.testCustomizer = new ConcurrentDataBrokerTestCustomizer(true);
        this.dataBroker = this.testCustomizer.createDataBroker();
        this.domBroker = this.testCustomizer.createDOMDataBroker();
        this.testCustomizer.updateSchema(runtimeContext);
        this.schemaContext = context;
        setupWithDataBroker(this.dataBroker);
        rpcRouter = new DOMRpcRouter(getSchemaService());
        bnnc = new DefaultBindingDOMCodecFactory().createBindingDOMCodec(runtimeContext);
        codecFactory = new JsonRpcCodecFactory(context);
    }

    @Override
    protected Set<YangModuleInfo> getModuleInfos() {
        return Set.of(
            BindingRuntimeHelpers.getYangModuleInfo(Config.class),
            BindingRuntimeHelpers.getYangModuleInfo(NetworkTopology.class),
            BindingRuntimeHelpers.getYangModuleInfo(TopContainer.class),
            BindingRuntimeHelpers.getYangModuleInfo(Notification1.class),
            BindingRuntimeHelpers.getYangModuleInfo(FactorialInput.class));
    }

    protected BindingNormalizedNodeSerializer getCodec() {
        return bnnc;
    }

    protected DOMSchemaService getSchemaService() {
        return this.testCustomizer.getSchemaService();
    }

    @Override
    protected void setupWithDataBroker(final DataBroker broker) {
    }

    @Override
    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    @Override
    public DOMDataBroker getDomBroker() {
        return this.domBroker;
    }

    public DOMMountPointService getDOMMountPointService() {
        return this.domMountPointService;
    }

    public DOMRpcRouter getDOMRpcRouter() {
        return rpcRouter;
    }

    public DOMNotificationRouter getDOMNotificationRouter() {
        return notificationRouter;
    }

    public BindingRuntimeContext getBindingRuntimeContext() {
        return runtimeContext;
    }

    protected void logTestName(final String stage) {
        LOG.info("{}", Strings.repeat("=", 80));
        LOG.info("[{}]{}", stage, nameRule.getMethodName());
        LOG.info("{}", Strings.repeat("=", 80));
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public static void retryAction(final TimeUnit timeUnit, final long wait, final Callable<Boolean> action)
            throws InterruptedException {
        int counter = 1;
        long future = System.currentTimeMillis() + timeUnit.toMillis(wait);
        while (System.currentTimeMillis() < future) {
            try {
                LOG.info("Try #{}", counter++);
                if (action.call()) {
                    return;
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                LOG.error("Action failed, ignore and retry", e);
            }
            TimeUnit.MILLISECONDS.sleep(300);
        }
        throw new AssertionFailedError("Action didnt suceed within specified time range");
    }

    protected static int getFreeTcpPort() {
        try (var socket = new Socket()) {
            socket.bind(null);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
