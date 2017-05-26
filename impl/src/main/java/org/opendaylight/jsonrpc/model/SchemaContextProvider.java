/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.rev161201.Peer;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * API for creating {@link SchemaContext} for given {@link Peer}
 * 
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public interface SchemaContextProvider {
    /**
     * Create {@link SchemaContext} for given {@link Peer}. This method never
     * returns null.
     * 
     * @param peer {@link Peer} instance to create {@link SchemaContext} for
     * @return {@link SchemaContext}
     */
    SchemaContext createSchemaContext(Peer peer);
}
