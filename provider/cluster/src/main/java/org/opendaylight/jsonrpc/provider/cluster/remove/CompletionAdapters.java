/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.provider.cluster.remove;

import akka.dispatch.OnComplete;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collection;
import org.opendaylight.jsonrpc.model.RpcExceptionImpl;
import org.opendaylight.jsonrpc.provider.cluster.messages.EmptyResultReply;
import org.opendaylight.jsonrpc.provider.cluster.messages.InvokeRpcResponse;
import org.opendaylight.jsonrpc.provider.cluster.messages.PathAndDataMsg;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yangtools.yang.common.RpcError;

public class CompletionAdapters {

    private static class RpcInvocation extends OnComplete<Object> {
        private final SettableFuture<DOMRpcResult> result;
        private final String peer;

        private RpcInvocation(SettableFuture<DOMRpcResult> result, String peer) {
            this.result = result;
            this.peer = peer;
        }

        @Override
        public void onComplete(final Throwable failure, final Object response) {
            if (failure != null) {
                if (failure instanceof RpcExceptionImpl) {
                    result.setException(failure);
                } else {
                    result.setException(new RpcExceptionImpl(
                            String.format("%s : RPC invocation failed", peer), failure));
                }
                return;
            }

            if (response instanceof EmptyResultReply) {
                result.set(null);
                return;
            }

            final Collection<? extends RpcError> errors = ((InvokeRpcResponse) response).getErrors();
            final PathAndDataMsg responseData = ((InvokeRpcResponse) response).getResponseData();
            final DOMRpcResult rpcResult;
            if (responseData == null) {
                rpcResult = new DefaultDOMRpcResult(ImmutableList.copyOf(errors));
            } else {
                rpcResult = new DefaultDOMRpcResult(responseData.getData(), errors);
            }
            result.set(rpcResult);
        }
    }

    public static OnComplete<Object> rpcInvocation(SettableFuture<DOMRpcResult> result, String peer) {
        return new RpcInvocation(result, peer);
    }
}
