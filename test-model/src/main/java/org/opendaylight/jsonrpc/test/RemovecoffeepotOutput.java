/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.test;

import com.google.gson.annotations.SerializedName;

public class RemovecoffeepotOutput {
    @SerializedName("cups-brewed")
    private java.lang.Long cupsBrewed;
    @SerializedName("drink")
    private java.lang.String drink;

    public RemovecoffeepotOutput(final java.lang.Long cupsBrewed, final java.lang.String drink) {
        this.cupsBrewed = cupsBrewed;
        this.drink = drink;
    }

    public java.lang.Long getCupsBrewed() {
        return this.cupsBrewed;
    }

    public java.lang.String getDrink() {
        return this.drink;
    }

    public void setCupsBrewed(final java.lang.Long cupsBrewed) {
        this.cupsBrewed = cupsBrewed;
    }

    public void setDrink(final java.lang.String drink) {
        this.drink = drink;
    }
}
