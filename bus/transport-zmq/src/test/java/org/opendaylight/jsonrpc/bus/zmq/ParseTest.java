/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.zmq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import org.junit.Test;
import org.opendaylight.jsonrpc.bus.api.SessionType;

public class ParseTest {
    @Test
    public void testDecodeReadyCommand() {
        ByteBuf buffer = Unpooled.buffer().writeBytes(ByteBufUtil
                .decodeHexDump("04260552454144590b536f636b65742d5479706500000003524551084964656e7469747900000000"));
        final ReadyCommand cmd = new ReadyCommand(buffer);
        assertEquals(2, cmd.getMetadata().size());
        assertEquals("READY", cmd.name());
    }

    @Test
    public void testEncodeReadyCommand() {
        final ByteBuf expected = Unpooled.buffer()
                .writeBytes(ByteBufUtil.decodeHexDump("04190552454144590b536f636b65742d5479706500000003524551"));
        final ReadyCommand cmd = new ReadyCommand(SessionType.REQ);
        assertTrue(ByteBufUtil.equals(expected, cmd.toBuffer()));
        assertEquals(1, cmd.getMetadata().size());
        assertEquals("READY", cmd.name());
    }

    @Test
    public void testDecodeSubscribeMessage() {
        ByteBuf buffer = Unpooled.buffer().writeBytes(ByteBufUtil.decodeHexDump("000401585858"));
        SubscribeMessage cmd = new SubscribeMessage(buffer);
        assertEquals("XXX", cmd.topic());
    }

    @Test
    public void testEncodeSubscribeMessage() {
        ByteBuf expected = Unpooled.buffer().writeBytes(ByteBufUtil.decodeHexDump("000401585858"));
        SubscribeMessage cmd = new SubscribeMessage("XXX");
        assertTrue(ByteBufUtil.equals(expected, cmd.toBuffer()));
    }
}
