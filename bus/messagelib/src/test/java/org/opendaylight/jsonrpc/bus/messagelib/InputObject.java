/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

public class InputObject {
    private int propertyA;
    private float propertyB;
    private String propertyC;
    private NestedObject propertyD;

    public int getPropertyA() {
        return propertyA;
    }

    public void setPropertyA(int propertyA) {
        this.propertyA = propertyA;
    }

    public float getPropertyB() {
        return propertyB;
    }

    public void setPropertyB(float propertyB) {
        this.propertyB = propertyB;
    }

    public String getPropertyC() {
        return propertyC;
    }

    public void setPropertyC(String propertyC) {
        this.propertyC = propertyC;
    }

    public NestedObject getPropertyD() {
        return propertyD;
    }

    public void setPropertyD(NestedObject propertyD) {
        this.propertyD = propertyD;
    }

    @Override
    public String toString() {
        return "InputObject [propertyA=" + getPropertyA() + ", propertyB=" + getPropertyB() + ", propertyC="
                + getPropertyC() + ", propertyD=" + getPropertyD() + "]";
    }
}
