/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.hmap;

/**
 * Path <strong>co</strong>der/<strong>dec</strong>oder. It transforms between P
 * and {@link Iterable} of I. Instances of {@link PathCodec} must ensure that
 * <ol>
 * <li>they are thread safe, that is they work safely in multi-threaded
 * environment</li>
 * <li>they are re-usable, so single instance can be reused for multiple
 * invocations</li>
 * </ol>
 *
 * @param <P> external path representation
 * @param <I> internal node identifier
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 */
public interface PathCodec<P, I> {
    /**
     * Serialize external path specification (P) into {@link Iterable} of I
     *
     * @param path external path specification
     * @return {@link Iterable} of I
     */
    Iterable<I> serialize(P path);

    /**
     * Deserialize sequence of I into external path specification P.
     *
     * @param path {@link Iterable}&lt;I&gt; sequence of internal path
     *            identifiers
     * @return P external path specification
     */
    P deserialize(Iterable<I> path);
}
