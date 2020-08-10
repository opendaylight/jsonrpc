/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.time.Instant;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.dom.api.DOMEvent;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

@Deprecated(forRemoval = true)
public class JsonRpcNotification implements DOMNotification, DOMEvent {
    private final ContainerNode content;
    private final Absolute schemaPath;
    private final Instant eventTime;

    public JsonRpcNotification(final ContainerNode content, final Instant eventTime, final QName schemaPath) {
        this.content = content;
        this.eventTime = eventTime;
        this.schemaPath = Absolute.of(schemaPath);
    }

    public JsonRpcNotification(final ContainerNode content, final QName schemaPath) {
        this(content, Instant.now(), schemaPath);
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
    public Instant getEventInstant() {
        return eventTime;
    }

    @Override
    public String toString() {
        return "JsonRpcNotification [eventTime=" + eventTime + ", content=" + content + ", schemaPath=" + schemaPath
                + "]";
    }
}
