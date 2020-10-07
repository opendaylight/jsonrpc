/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.time.Instant;
import java.util.Date;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.dom.api.DOMEvent;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

public class JsonRpcNotification implements DOMNotification, DOMEvent {
    private final ContainerNode content;
    private final Absolute schemaPath;
    private final Date eventTime;

    public JsonRpcNotification(final ContainerNode content, final Date eventTime, final QName schemaPath) {
        this.content = content;
        this.eventTime = (Date) eventTime.clone();
        this.schemaPath = Absolute.of(schemaPath);
    }

    @NonNull
    @Override
    public Absolute getType() {
        return schemaPath;

    }

    @NonNull
    @Override
    public ContainerNode getBody() {
        return content;
    }

    @Override
    public String toString() {
        return "JsonRpcNotification [eventTime=" + eventTime + ", content=" + content + ", schemaPath=" + schemaPath
                + "]";
    }

    @Override
    public Instant getEventInstant() {
        return Instant.ofEpochMilli(eventTime.getTime());
    }
}
