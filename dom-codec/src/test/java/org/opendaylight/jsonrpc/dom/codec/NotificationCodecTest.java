/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonElement;
import java.io.IOException;
import org.junit.Test;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;

public class NotificationCodecTest extends AbstractCodecTest {
    @Test
    public void testDecode() throws IOException {
        JsonElement input = PARSER.parse("{ \"current-level\": 10, \"max-level\" : 20}");
        Codec<JsonElement, DOMNotification, IOException> codec = factory
                .notificationCodec(getNotification("notification1"));

        final DOMNotification decoded = codec.deserialize(input);

        JsonElement encoded = codec.serialize(decoded);

        assertEquals(input, encoded);
    }

    private NotificationDefinition getNotification(String name) {
        return schemaContext.getNotifications()
                .stream()
                .filter(notif -> notif.getQName().getLocalName().equals(name))
                .findFirst()
                .orElseThrow();
    }

}
