/*
 * Copyright (c) 2018 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNodes;

/**
 * Simple implementation of {@link DataTreeCandidate} just for purpose of DCN.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since May 8, 2018
 */
public class DataTreeCandidateImpl implements DataTreeCandidate {
    private DataTreeCandidateNode node;
    private YangInstanceIdentifier path;

    public DataTreeCandidateImpl(YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
        node = DataTreeCandidateNodes.written(data);
        this.path = path;
    }

    @Override
    public DataTreeCandidateNode getRootNode() {
        return node;
    }

    @Override
    public YangInstanceIdentifier getRootPath() {
        return path;
    }
}
