/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.util.Objects;
import java.util.Set;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;

/**
 * Data change notification object.
 *
 * @see DOMDataTreeChangeListener
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Apr 17, 2018
 */
public class DataChangeNotification {
    private final Set<JSONRPCArg> changes;

    public DataChangeNotification(Set<JSONRPCArg> changes) {
        this.changes = Objects.requireNonNull(changes);
    }

    public Set<JSONRPCArg> getChanges() {
        return changes;
    }

    @Override
    public String toString() {
        return "DataChangeNotification [changes=" + getChanges() + "]";
    }
}
