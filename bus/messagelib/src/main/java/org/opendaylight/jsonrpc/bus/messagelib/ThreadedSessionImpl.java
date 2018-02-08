/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opendaylight.jsonrpc.bus.BusSession;
import org.opendaylight.jsonrpc.bus.BusSessionMsgHandler;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcErrorObject;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcException;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadedSessionImpl<T extends AutoCloseable>
        implements Runnable, BusSessionMsgHandler, NotificationMessageHandler, RequestMessageHandler, ThreadedSession {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadedSessionImpl.class);
    private static final ExecutorService THREAD_POOL = Executors
            .newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Bus-session-%d").setDaemon(true).build());
    private final Object handler;
    private final Future<?> future;
    private final Session session;

    ThreadedSessionImpl(MessageLibrary messaging, BusSession busSession, T handler) {
        this.handler = Preconditions.checkNotNull(handler);
        session = new Session(messaging, busSession);
        session.setNotificationMessageHandler(this);
        session.setRequestMessageHandler(this);
        future = THREAD_POOL.submit(this);
    }

    @Override
    public ListenableFuture<Void> stop() {
        session.stopLoop();

        final ListenableFuture<Void> resultFuture = Futures.transform(JdkFutureAdapters.listenInPoolThread(future),
            r -> null, MoreExecutors.directExecutor());
        Futures.addCallback(resultFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void notUsed) {
                    LOG.debug("Successfully stopped session {}", session);
                    session.close();
                }

                @Override
                public void onFailure(Throwable failure) {
                    LOG.error("Error stopping session {}", session, failure);
                    session.close();
                }
        }, MoreExecutors.directExecutor());

        return resultFuture;
    }

    /**
     * Match method based on name: For example, method name in JSON-RPC message
     * is 'method-abc'. Match will occur when java method:
     * <ol>
     * <li>is named 'methodAbc'</li>
     * <li>is named 'method_abc'</li>
     * </ol>
     * the number of method arguments is not a factor in the match
     */
    private static class NameMatchingPredicate implements Predicate<Method> {
        protected final JsonRpcRequestMessage msg;

        NameMatchingPredicate(JsonRpcRequestMessage msg) {
            this.msg = msg;
        }

        @Override
        public boolean test(Method method) {
            boolean nameMatched = false;
            nameMatched |= method.getName().equalsIgnoreCase(toUnderscoreName(msg.getMethod()));
            nameMatched |= method.getName().equalsIgnoreCase(toCamelCaseName(msg.getMethod()));
            return nameMatched;
        }

        /**
         * Convert raw method name to name with underscores. <br />
         * <p>
         * Examples:
         * </p>
         * <ul>
         * <li>method-abc =&gt; method_abc</li>
         * <li>method_def =&gt; method_def</li>
         * <li>method123 => method123</li>
         * </ul>
         */
        private String toUnderscoreName(String name) {
            return name.replaceAll("-", "_");
        }

        /**
         * Convert raw method name to one with camel case.
         */
        private String toCamelCaseName(String name) {
            final String ret = Arrays.asList(name.split("_|-")).stream().map(n -> {
                if (n.length() > 0) {
                    return n.substring(0, 1).toUpperCase() + n.substring(1);
                } else {
                    return n.toUpperCase();
                }
            }).collect(Collectors.joining());
            return Character.toLowerCase(ret.charAt(0)) + ret.substring(1);
        }
    }


    /**
     * A matcher that not only uses the method name but also the number of arguments.
     */
    private static class StrictMatchingPredicate extends NameMatchingPredicate {

        StrictMatchingPredicate(JsonRpcRequestMessage msg) {
            super(msg);
        }

        private boolean nameMatches(Method method) {
            return super.test(method);
        }

        private boolean parameterCountMatches(Method method) {
            return getParametersCount(msg) == method.getParameterTypes().length;
        }

        @Override
        public boolean test(Method method) {
            return nameMatches(method) && parameterCountMatches(method);
        }
    }

    /**
     * Sorts list of matched methods based on preference. Currently it only
     * prefers method without underscore in it's name
     */
    private static Comparator<Method> nameSorter() {
        return (o1, o2) -> {
            if (o1.getName().contains("_")) {
                return 1;
            }
            if (o2.getName().contains("_")) {
                return -1;
            }
            return o1.getName().compareTo(o2.getName());
        };
    }

    private static int getParametersCount(JsonRpcRequestMessage msg) {
        int size = 0;
        if (msg.getParams() instanceof JsonArray) {
            size = ((JsonArray) msg.getParams()).size();
        }
        if (msg.getParams() instanceof JsonPrimitive) {
            size = 1;
        }
        return size;
    }

    private List<Method> findMethodStrict(JsonRpcRequestMessage msg) {
        return Arrays.stream(handler.getClass().getDeclaredMethods())
                .filter(new StrictMatchingPredicate(msg))
                .sorted(nameSorter())
                .collect(Collectors.toList());
    }

    private List<Method> findMethodLenient(JsonRpcRequestMessage msg) {
        return Arrays.stream(handler.getClass().getDeclaredMethods())
                .filter(new NameMatchingPredicate(msg))
                .sorted(nameSorter())
                .collect(Collectors.toList());
    }

    /*
     * Create array of values to be used as method arguments. This method is
     * assuming that number of parameters is already matching number of
     * arguments
     */
    private Object[] getArgumentsForMethod(Method method, JsonRpcRequestMessage message) throws JsonRpcException {
        final Object[] args = new Object[getParametersCount(message)];
        final Class<?>[] argsTypes = method.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            args[i] = message.getParamsAtIndexAsObject(i, argsTypes[i]);
        }
        return args;
    }

    @SuppressWarnings("squid:S1166")
    private Object invokeHandler(JsonRpcRequestMessage message)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        List<Method> opt = findMethodStrict(message);
        if (!opt.isEmpty()) {
            // We have a method with the incoming method name and have
            // managed to parse params as per that method's signature.
            // This could be fooled if the parsing of incoming params
            // somehow works for a different set of params. (i.e.
            // objects with members similar to another might be parsed
            // as one or other
            for (final Method m : opt) {
                try {
                    Object[] args = getArgumentsForMethod(m, message);
                    return m.invoke(handler, args);
                } catch (JsonRpcException e) {
                    String msg = String.format("Failed to manage arguments when invoking method %s", m);
                    LOG.debug(msg);
                    throw new IllegalArgumentException(msg, e);
                }
            }
        }

        // At this point, could be either wrong number of arguments or wrong argument types

        opt = findMethodLenient(message);
        if (!opt.isEmpty()) {
            String msg = String.format("Found method but wrong number of arguments: %s", message.getMethod());
            LOG.debug(msg);
            throw new IllegalArgumentException(msg);
        }

        throw new NoSuchMethodException(String.format("Method not found : %s", message.getMethod()));
    }

    /**
     * Handles JSON-RPC request. If handler instance provided in constructor is
     * instance of {@link RequestMessageHandler} then handling is delegated to
     * it instead.
     *
     * @see NotificationMessageHandler#handleNotification(JsonRpcRequestMessage)
     */
    @Override
    @SuppressWarnings("squid:S1166")
    public void handleNotification(JsonRpcRequestMessage notification) {
        try {
            if (handler instanceof NotificationMessageHandler) {
                ((NotificationMessageHandler) handler).handleNotification(notification);
            } else {
                invokeHandler(notification);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // We don't care if there are any exceptions.
            // No way to tell the publisher.
            LOG.error("Can't map notification to method : {}", notification.getMethod(), e);
        }
    }

    /**
     * Handles JSON-RPC request. If handler instance provided in constructor is
     * instance of {@link RequestMessageHandler} then handling is delegated to
     * it instead.
     *
     * @see RequestMessageHandler#handleRequest(JsonRpcRequestMessage,
     *      JsonRpcReplyMessage.Builder)
     */
    @Override
    @SuppressWarnings("squid:S1166")
    public void handleRequest(JsonRpcRequestMessage request, JsonRpcReplyMessage.Builder replyBuilder) {
        if (handler instanceof RequestMessageHandler) {
            ((RequestMessageHandler) handler).handleRequest(request, replyBuilder);
            return;
        }
        try {
            Object response = invokeHandler(request);
            replyBuilder.resultFromObject(response);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            LOG.error("Request method not found: {}", request.getMethod());
            JsonRpcErrorObject error = new JsonRpcErrorObject(-32601, "Method not found", null);
            replyBuilder.error(error);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid arguments");
            JsonRpcErrorObject error = new JsonRpcErrorObject(-32602, "Invalid params", null);
            replyBuilder.error(error);
        } catch (InvocationTargetException e) {
            LOG.error("Error while executing method: {}", request.getMethod());
            JsonRpcErrorObject error = new JsonRpcErrorObject(-32000, getErrorMessage(e), null);
            replyBuilder.error(error);
        }
    }

    /*
     * Extract the error message from an exception.
     * First check if there is a nested exception.
     * If no error message is available, then use a fixed string.
     */
    private String getErrorMessage(Exception error) {
        String errorMessage;
        Throwable inner = error.getCause();
        if (inner != null) {
            errorMessage = inner.getMessage();
        } else {
            errorMessage = error.getMessage();
        }
        // Ensure some error string is sent
        if (errorMessage == null) {
            errorMessage = JsonRpcErrorObject.JSONRPC_ERROR_MESSAGE_INTERNAL;
        }
        return errorMessage;
    }

    @Override
    public int handleIncomingMsg(String message) {
        // Let session parse the message. It will call back one of handleXYZ
        // functions above.
        try {
            session.processIncomingMessage(message);
        } catch (MessageLibraryMismatchException e) {
            LOG.error("Invalid message received", e);
        }
        // One of the message handling method might have raised interrupt on
        // this thread.
        return Thread.currentThread().isInterrupted() ? -1 : 0;
    }

    @Override
    public void run() {
        // decorate thread name so we can identify it in thread dump
        final String threadName = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(threadName + " [" + session.getSessionType().name() + "]");
            LOG.trace("Thread starting {}", future);
            session.startLoop(this);
        } finally {
            Thread.currentThread().setName(threadName);
        }
    }
}
