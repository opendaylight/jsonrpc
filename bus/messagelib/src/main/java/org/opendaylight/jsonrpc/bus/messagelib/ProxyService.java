/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.lang.reflect.InvocationHandler;

public interface ProxyService extends InvocationHandler {

    <T extends AutoCloseable> T createRequesterProxy(String uri, Class<T> cls);

    <T extends AutoCloseable> T createRequesterProxy(String uri, Class<T> cls, int timeout);

    <T extends AutoCloseable> T createPublisherProxy(String uri, Class<T> cls);

    <T extends AutoCloseable> T createPublisherProxy(String uri, Class<T> cls, int timeout);
}
