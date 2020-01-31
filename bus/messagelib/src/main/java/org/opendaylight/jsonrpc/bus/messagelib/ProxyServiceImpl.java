/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.opendaylight.jsonrpc.bus.messagelib.MessageLibraryConstants.DEFAULT_SKIP_ENDPOINT_CACHE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.JsonElement;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.opendaylight.jsonrpc.bus.api.RecoverableTransportException;
import org.opendaylight.jsonrpc.bus.api.RpcMethod;
import org.opendaylight.jsonrpc.bus.api.UnrecoverableTransportException;
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
    private final ConcurrentMap<Object, BaseSession> proxyMap = new MapMaker().weakKeys().makeMap();
    private final MessageLibrary messaging;

    public ProxyServiceImpl(MessageLibrary messaging) {
        this.messaging = Objects.requireNonNull(messaging);
    }

    @Override
    public <T extends AutoCloseable> T createRequesterProxy(String uri, Class<T> cls) {
        return createRequesterProxy(uri, cls, DEFAULT_SKIP_ENDPOINT_CACHE);
    }

    @Override
    public <T extends AutoCloseable> T createRequesterProxy(String uri, Class<T> cls, boolean skipCache) {
        final RequesterSession session = messaging.requester(uri, NoopReplyMessageHandler.INSTANCE, skipCache);
        final T obj = getProxySafe(cls);
        proxyMap.put(obj, session);
        return obj;
    }

    @Override
    public <T extends AutoCloseable> T createPublisherProxy(String uri, Class<T> cls) {
        return createPublisherProxy(uri, cls, true);
    }

    @Override
    public <T extends AutoCloseable> T createPublisherProxy(String uri, Class<T> cls, boolean skipCache) {
        final PublisherSession session = messaging.publisher(uri, skipCache);
        final T obj = getProxySafe(cls);
        proxyMap.put(obj, session);
        return obj;
    }

    private void deleteProxy(Object obj) {
        final BaseSession session = proxyMap.remove(obj);
        if (session != null) {
            session.close();
        }
    }

    @VisibleForTesting
    static String getMethodName(Method method) {
        if (method.isAnnotationPresent(RpcMethod.class)) {
            return method.getAnnotation(RpcMethod.class).value();
        } else {
            return method.getName();
        }
    }

    /**
     * Unwrap parameter out of array if method has only one argument and only one element exists in array. Reason for
     * this operation is that when array is passed down to serialization, object will be wrapped in json array. This is
     * not necessary problem if both ends uses same logic, but will break when not.
     *
     * <p>Example (when unwrap is not applied):
     *
     * <code>
     *    {"jsonrpc":"2.0","id":1,"method":"methodX","params":[{"property":"value"}]}
     * </code>
     *
     * <p> this should be
     * <code>
     *    {"jsonrpc":"2.0","id":1,"method":"methodX","params":{"property":"value"}}
     * </code>
     * or
     * <code>
     *    {"jsonrpc":"2.0","id":1,"method":"methodX","params":"value"}
     * </code>
     *
     * @param method {@link Method} used for invocation
     * @param params array of input arguments
     * @return unwrapped object or original array if conditions are not met
     */
    static Object unwrapIfNecessary(Method method, Object[] params) {
        return (method.getParameterCount() == 1 && params != null && params.length == 1) ? params[0] : params;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public Object invoke(Object obj, Method method, Object[] params) {
        final String methodName = getMethodName(method);
        final BaseSession session = proxyMap.get(obj);
        final Object unwrappedArgs = unwrapIfNecessary(method, params);
        /*
         * Special case to handle #toString() method invocation. It is
         * undesirable to dispatch such method call via JSON-RPC, so we are
         * providing some hint here about object state.
         */
        if (TO_STRING_METHOD_NAME.equals(methodName) && method.getParameterTypes().length == 0) {
            LOG.debug("Proxy for session {}", proxyMap.get(obj));
            return String.format("Proxy => %s", proxyMap.get(obj));
        }
        /*
         * Special case to handle AutoCloseable#close(). Instead of forwarding
         * message to bus, proxied object state is cleaned from internal
         * structures.
         */
        if (CLOSE_METHOD_NAME.equals(methodName) && method.getParameterTypes().length == 0) {
            LOG.debug("Cleaning up proxy instance {}", proxyMap.get(obj));
            deleteProxy(obj);
            return null;
        }
        /*
         * Special case to delegate invocation to default method.
         */
        if (method.isDefault()) {
            try {
                return MethodHandles.lookup()
                        .findSpecial(method.getDeclaringClass(), method.getName(),
                                MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                                method.getDeclaringClass())
                        .bindTo(obj)
                        .invokeWithArguments(params);
            }
            catch (ProxyServiceGenericException e) {
                throw e;
            }
            catch (Throwable e) {
                throw new IllegalStateException("Fail to delegate invocation to default method", e);
            }
        }
        if (session instanceof PublisherSession) {
            if (!method.getReturnType().equals(void.class)) {
                throw new ProxyServiceGenericException("Method expects return value for publisher.");
            } else {
                ((PublisherSession) session).publish(methodName, unwrappedArgs);
                // no return value for notifications
                return null;
            }
        }
        if (session instanceof RequesterSession) {
            final int configuredRetryCount = ((RequesterSession) session).retryCount();
            final long configuredRetryDelay = ((RequesterSession) session).retryDelay();
            int retry = configuredRetryCount;
            for (;;) {
                try {
                    if (configuredRetryCount == retry) {
                        Util.awaitForTransport((ClientSession) session, 2000L);
                    }
                    final JsonRpcReplyMessage reply = ((RequesterSession) session).sendRequestAndReadReply(methodName,
                            unwrappedArgs);
                    return getReturnFromReplyMessage(method, (JsonRpcReplyMessage) reply);
                } catch (RecoverableTransportException e) {
                    if (retry-- > 0) {
                        LOG.debug("Request to {} failed, will retry ({}/{})", session, configuredRetryCount - retry,
                                configuredRetryCount, e);
                        Uninterruptibles.sleepUninterruptibly(configuredRetryDelay, TimeUnit.MILLISECONDS);
                    } else {
                        throw new UnrecoverableTransportException(
                                "Request failed after " + configuredRetryCount + " tries", e);
                    }
                }
            }
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
            try {
                return replyMsg.getResultAsObject(method.getGenericReturnType());
            } catch (JsonRpcException e) {
                throw new ProxyServiceGenericException(e);
            }
        }
    }

    @Override
    public Optional<BaseSession> getProxySession(Object proxy)  {
        return Optional.ofNullable(proxyMap.get(proxy));
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
