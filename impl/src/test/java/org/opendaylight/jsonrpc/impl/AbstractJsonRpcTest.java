/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractSchemaAwareTest;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonParser;

import junit.framework.AssertionFailedError;

/**
 * Common test infrastructure for {@link SchemaContext} aware tests.
 * 
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
abstract class AbstractJsonRpcTest extends AbstractSchemaAwareTest {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractJsonRpcTest.class);
    private DataBrokerTestCustomizer testCustomizer;
    private DataBroker dataBroker;
    private DOMDataBroker domBroker;
    protected SchemaContext schemaContext;
    protected final JsonParser jsonParser = new JsonParser();

    protected void setupWithSchema(SchemaContext context) {
        this.testCustomizer = createDataBrokerTestCustomizer();
        this.dataBroker = this.testCustomizer.createDataBroker();
        this.domBroker = this.testCustomizer.createDOMDataBroker();
        this.testCustomizer.updateSchema(context);
        this.schemaContext = context;
        setupWithDataBroker(this.dataBroker);
    }

    protected DOMSchemaService getSchemaService() {
        return this.testCustomizer.getSchemaService();
    }

    protected void setupWithDataBroker(DataBroker dataBroker) {
    }

    protected DataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        return new DataBrokerTestCustomizer();
    }

    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    public DOMDataBroker getDomBroker() {
        return this.domBroker;
    }

    public DOMMountPointService getDOMMountPointService() {
        return this.testCustomizer.getDOMMountPointService();
    }

    protected static final void assertCommit(ListenableFuture<Void> commit)
            throws ExecutionException, TimeoutException {
        try {
            commit.get(500L, TimeUnit.MILLISECONDS);
        } catch (java.lang.InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

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
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                LOG.error("Action failed, ignore and retry : {}", e.getMessage());
            }
        }
        throw new AssertionFailedError("Action didnt suceed within specified time range");
    }

    protected static int getFreeTcpPort() {
        int port = -1;
        try {
            java.net.Socket s = new java.net.Socket();
            s.bind(null);
            port = s.getLocalPort();
            s.close();
            return port;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
