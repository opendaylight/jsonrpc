/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

public interface ApiWithDefaultMethods extends AutoCloseable {
    default int sum(int left, int right) {
        return sum(new Dto(left, right));
    }

    int sum(Dto dto);

    class Dto {
        private final int sum;

        public int sum() {
            return sum;
        }

        public Dto(int left, int right) {
            sum = left + right;
        }
    }
}
