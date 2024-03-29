/*
 * Copyright (c) 2021 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

public interface Codec<P, I, X extends Exception> {
    I deserialize(P input) throws X;

    P serialize(I input) throws X;
}
