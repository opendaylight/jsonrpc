/*
 * Copyright (c) 2020 dNation.cloud. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.dom.codec;

import com.google.common.annotations.Beta;
import java.util.Iterator;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

@Beta
public abstract class PathWalker<V> {
    protected final YangInstanceIdentifier path;

    public PathWalker(YangInstanceIdentifier path) {
        this.path = path;
    }

    public V walk() {
        final Iterator<PathArgument> it = path.getPathArguments().iterator();
        while (it.hasNext()) {
            visitPathArgument(it.next());
        }
        return result();
    }

    protected abstract void visitPathArgument(PathArgument arg);

    protected abstract V result();
}
