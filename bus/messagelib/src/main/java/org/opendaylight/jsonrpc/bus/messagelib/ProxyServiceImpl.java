/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.gson.JsonElement;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcException;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ProxyService}.
 *
 * @author Shaleen Saxena
 *
 */
public class ProxyServiceImpl implements ProxyService {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyServiceImpl.class);

    private static final String TO_STRING_METHOD_NAME = "toString";
    private static final String CLOSE_METHOD_NAME = "close";

    private final Map<Object, BaseSession> proxyMap = Collections.synchronizedMap(new IdentityHashMap<>());
    private final MessageLibrary messaging;

    public ProxyServiceImpl(MessageLibrary messaging) {
        this.messaging = Objects.requireNonNull(messaging);
    }

    @Override
    public <T extends AutoCloseable> T createRequesterProxy(String uri, Class<T> cls) {
        final RequesterSession session = messaging.requester(uri, NoopReplyMessageHandler.INSTANCE);
        final T obj = getProxySafe(cls);
        proxyMap.put(obj, session);
        return obj;
    }

    @Override
    public <T extends AutoCloseable> T createRequesterProxy(String uri, Class<T> cls, int timeout) {
        final T proxy = createRequesterProxy(uri, cls);
        setTimeout(proxy, timeout);
        return proxy;
    }

    @Override
    public <T extends AutoCloseable> T createPublisherProxy(String uri, Class<T> cls) {
        final PublisherSession session = messaging.publisher(uri);
        final T obj = getProxySafe(cls);
        proxyMap.put(obj, session);
        return obj;
    }

    @Override
    public <T extends AutoCloseable> T createPublisherProxy(String uri, Class<T> cls, int timeout) {
        T proxy = createPublisherProxy(uri, cls);
        setTimeout(proxy, timeout);
        return proxy;
    }

    private void deleteProxy(Object obj) {
        final BaseSession session = proxyMap.remove(obj);
        if (session != null) {
            session.close();
        }
    }

    private void setTimeout(Object obj, int time) {
        final BaseSession session = proxyMap.get(obj);
        if (session != null) {
            session.setTimeout(time);
        }
    }

    @Override
    public Object invoke(Object obj, Method method, Object[] params) {
        final String methodName = method.getName();
        final BaseSession session = proxyMap.get(obj);

        /*
         * Special case to handle #toString() method invocation. It is
         * undesirable to dispatch such method call via JSON-RPC, so we are
         * providing some hint here about object state.
         */
        if (TO_STRING_METHOD_NAME.equals(methodName) && method.getParameterTypes().length == 0) {
            LOG.debug("Proxy for session {}", proxyMap.get(obj));
            return null;
        }
        /*
         * Special case to handle AutoCloseable#close(). Instead of forwarding
         * message to bus, proxied object state is cleaned from internal
         * structures.
         */
        if (CLOSE_METHOD_NAME.equals(methodName) && method.getParameterTypes().length == 0) {
            LOG.debug("Cleaning up proxy instance {}", obj);
            deleteProxy(obj);
            return null;
        }
        if (session instanceof PublisherSession) {
            if (!method.getReturnType().equals(void.class)) {
                throw new ProxyServiceGenericException("Method expects return value for publisher.");
            } else {
                ((PublisherSession) session).publish(methodName, params);
                // no return value for notifications
                return null;
            }
        }
        if (session instanceof RequesterSession) {
            final JsonRpcReplyMessage reply = ((RequesterSession) session).sendRequestAndReadReply(methodName,
                    params);
            return getReturnFromReplyMessage(method, (JsonRpcReplyMessage) reply);
        }
        throw new ProxyServiceGenericException("Logic error");
    }

    private Object getReturnFromReplyMessage(Method method, JsonRpcReplyMessage replyMsg) {
        if (replyMsg.isError()) {
            JsonRpcErrorObject error = replyMsg.getError();
            throw new ProxyServiceGenericException(error.getMessage(), error.getCode());
        }

        if (method.getReturnType().equals(void.class)) {
            // We don't care what the reply is since a response is not expected.
            return null;
        } else if (method.getReturnType().equals(JsonElement.class)) {
            // no need to convert the result.
            return replyMsg.getResult();
        } else {
            // convert result to expected return type.
            Object result = null;
            try {
                result = replyMsg.getResultAsObject(method.getReturnType());
            } catch (JsonRpcException e) {
                throw new ProxyServiceGenericException(e);
            }
            return result;
        }
    }

    /*
     * Eliminate excessive "unchecked" warnings by extracting problematic part
     * into this method
     */
    @SuppressWarnings("unchecked")
    private <T extends AutoCloseable> T getProxySafe(Class<T> cls) {
        return (T) Proxy.newProxyInstance(cls.getClassLoader(), new Class[] { cls }, this);
    }
}
