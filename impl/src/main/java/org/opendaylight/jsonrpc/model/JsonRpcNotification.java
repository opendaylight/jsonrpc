/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.util.Date;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class JsonRpcNotification implements DOMNotification, DOMEvent {
    private final ContainerNode content;
    private final SchemaPath schemaPath;
    private final Date eventTime;

    public JsonRpcNotification(final ContainerNode content, final Date eventTime, final SchemaPath schemaPath) {
        this.content = content;
        this.eventTime = (Date) eventTime.clone();
        this.schemaPath = schemaPath;
    }

    @Nonnull
    @Override
    public SchemaPath getType() {
        return schemaPath;

    }

    @Nonnull
    @Override
    public ContainerNode getBody() {
        return content;
    }

    @Override
    public Date getEventTime() {
        return (Date) eventTime.clone();
    }

    @Override
    public String toString() {
        return "JsonRpcNotification [eventTime=" + eventTime + ", content=" + content + ", schemaPath=" + schemaPath
                + "]";
    }
}
