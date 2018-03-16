/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

public class NestedObject {
    private String propertyD;

    public String getPropertyD() {
        return propertyD;
    }

    public void setPropertyD(String propertyD) {
        this.propertyD = propertyD;
    }

    @Override
    public String toString() {
        return "NestedObject [propertyD=" + propertyD + "]";
    }
}
