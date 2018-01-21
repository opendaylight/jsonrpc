/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.opendaylight.jsonrpc.bus.SessionType;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcBaseMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcException;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcMessageError;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

/**
 * This service provides clients with an ability to define a proxy interface to
 * a server. The proxy interface must be same as the one defined by the server.
 * To define an interface for a notification server (i.e. a publisher), all
 * methods must return void, otherwise a {@link ProxyServiceGenericException} is
 * thrown.
 * 
 * @author Shaleen Saxena
 *
 */
public class ProxyServiceImpl implements ProxyService {
    private static final String TO_STRING_METHOD_NAME = "toString";
    private static final String CLOSE_METHOD_NAME = "close";
    private static final Logger LOG = LoggerFactory.getLogger(ProxyServiceImpl.class);
    private final Map<Object, Session> proxyMap;
    private final MessageLibrary messaging;

    public ProxyServiceImpl(MessageLibrary messaging) {
        this.messaging = Objects.requireNonNull(messaging);
        proxyMap = new IdentityHashMap<>();
    }

    @Override
    public <T extends AutoCloseable> T createRequesterProxy(String uri, Class<T> cls) {
        final Session session = messaging.requester(uri);
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
        final Session session = messaging.publisher(uri);
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
        if (proxyMap.containsKey(obj)) {
            try (final Session session = proxyMap.get(obj)) {
                proxyMap.remove(obj);
            }
        }
    }

    public void setTimeout(Object obj, int time) {
        if (proxyMap.containsKey(obj)) {
            proxyMap.get(obj).setTimeout(time);
        }
    }

    public int getTimeout(Object obj) {
        if (proxyMap.containsKey(obj)) {
            return proxyMap.get(obj).getTimeout();
        }
        return 0;
    }

    @Override
    public Object invoke(Object obj, Method method, Object[] params) {
        final String methodName = method.getName();
        final Session session = proxyMap.get(obj);
        String msg;

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

        if ((session.getSessionType() == SessionType.PUBLISHER) &&
            (!method.getReturnType().equals(void.class))) {
                throw new ProxyServiceGenericException("Method expects return value for publisher.");
        }

        try {
            msg = session.sendRequestAndReadReply(methodName, params);
        } catch (MessageLibraryTimeoutException e) {
            throw new ProxyServiceTimeoutException(e);
        } catch (MessageLibraryException e) {
            throw new ProxyServiceGenericException(e);
        }

        return getResultFromRequest(method, msg);
    }


    private Object getResultFromRequest(Method method, String msg) {
        if (msg == null)
            // nothing to do
            return null;

        // Parse reply and process.
        List<JsonRpcBaseMessage> replyList = JsonRpcSerializer.fromJson(msg);

        if (replyList.isEmpty()) {
            throw new ProxyServiceGenericException("Empty reply received");
        } else if (replyList.size() > 1) {
            throw new ProxyServiceGenericException("Extra responses receieved");
        }

        if (replyList.get(0) instanceof JsonRpcReplyMessage) {
            return getReturnFromReplyMessage(method, (JsonRpcReplyMessage) replyList.get(0));
        } else if (replyList.get(0) instanceof JsonRpcMessageError) {
            JsonRpcMessageError errorMsg = (JsonRpcMessageError) replyList.get(0);
            throw new ProxyServiceGenericException(errorMsg.getMessage(), errorMsg.getCode());
        }

        // We should not be here.
        throw new ProxyServiceGenericException("Unexpected reply");
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
