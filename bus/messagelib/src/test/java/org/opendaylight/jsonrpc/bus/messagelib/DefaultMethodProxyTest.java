/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.jsonrpc.bus.jsonrpc.JsonRpcReplyMessage;
import org.opendaylight.jsonrpc.bus.messagelib.ApiWithDefaultMethods.Dto;

/**
 * Test to verify that body of default method declared on proxied interface is invoked instead of being dispatched to
 * invocation handler.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Nov 7, 2019
 */
public class DefaultMethodProxyTest {
    @Test
    public void test() {
        MessageLibrary ml = mock(MessageLibrary.class);
        RequesterSession session = mock(RequesterSession.class);
        when(ml.requester(any(), any(), eq(true))).thenReturn(session);
        when(session.sendRequestAndReadReply(any(), any())).thenAnswer(new Answer<JsonRpcReplyMessage>() {
            @Override
            public JsonRpcReplyMessage answer(InvocationOnMock invocation) throws Throwable {
                return JsonRpcReplyMessage.builder()
                        .resultFromObject(((Dto) invocation.getArguments()[1]).sum())
                        .build();
            }
        });
        ProxyServiceImpl service = new ProxyServiceImpl(ml);
        ApiWithDefaultMethods proxy = service.createRequesterProxy("zmq://127.0.0.1:10000", ApiWithDefaultMethods.class,
                true);

        // call delegated to default interface method
        int result = proxy.sum(10, 5);
        assertEquals(15, result);

        // directly proxied call
        result = proxy.sum(new Dto(3, 7));
        assertEquals(10, result);
    }
}
