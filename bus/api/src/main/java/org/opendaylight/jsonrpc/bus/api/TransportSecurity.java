/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.api;

/**
 * Information about actual encryption used to secure communication channel.
 * When non-secure transport queries for this information, null is returned
 * instead.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jun 15, 2018
 */
public interface TransportSecurity {
    /**
     * Gets currently used cipher.
     *
     * @return cipher used to encrypt connection.
     */
    String cipher();

    /**
     * Get current secure protocol.
     *
     * @return protocol name
     */
    String protocol();
}
