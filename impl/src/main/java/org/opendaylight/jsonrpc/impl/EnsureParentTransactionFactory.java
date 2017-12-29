/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.impl.AbstractWriteTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.jsonrpc.model.ForwardingDOMDataWriteTransaction;
import org.opendaylight.jsonrpc.model.TransactionFactory;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Implementation of {@link TransactionFactory} which follows semantics of {@link AbstractWriteTransaction}.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
class EnsureParentTransactionFactory implements TransactionFactory {
    private final BindingToNormalizedNodeCodec codec;
    protected final DOMDataBroker domDataBroker;

    EnsureParentTransactionFactory(final DOMDataBroker domDataBroker, final BindingToNormalizedNodeCodec codec) {
        this.domDataBroker = domDataBroker;
        this.codec = codec;
    }

    @Override
    public DOMDataWriteTransaction get() {
        return new ForwardingDOMDataWriteTransaction(domDataBroker.newWriteOnlyTransaction()) {
            @Override
            public void merge(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
                ensureParentsByMerge(store, path);
                super.merge(store, path, data);
            }

            private void ensureParentsByMerge(final LogicalDatastoreType store,
                    final YangInstanceIdentifier normalizedPath) {
                List<PathArgument> currentArguments = new ArrayList<>();
                DataNormalizationOperation<?> currentOp = codec.getDataNormalizer().getRootOperation();
                Iterator<PathArgument> iterator = normalizedPath.getPathArguments().iterator();
                while (iterator.hasNext()) {
                    PathArgument currentArg = iterator.next();
                    try {
                        currentOp = currentOp.getChild(currentArg);
                    } catch (DataNormalizationException e) {
                        throw new IllegalArgumentException(
                                String.format("Invalid child encountered in path %s", normalizedPath), e);
                    }
                    currentArguments.add(currentArg);
                    YangInstanceIdentifier currentPath = YangInstanceIdentifier.create(currentArguments);

                    delegate.merge(store, currentPath, currentOp.createDefault(currentArg));
                }
            }
        };
    }

}
