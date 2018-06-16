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
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.spi.ForwardingDOMDataWriteTransaction;
import org.opendaylight.jsonrpc.model.TransactionFactory;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Implementation of {@link TransactionFactory} which follows semantics of
 * {@link AbstractWriteTransaction}.
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
        final DOMDataWriteTransaction delegateTx = domDataBroker.newWriteOnlyTransaction();
        return new ForwardingDOMDataWriteTransaction() {
            @Override
            public void merge(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data) {
                final List<PathArgument> currentArguments = new ArrayList<>();
                final Iterator<PathArgument> iterator = path.getPathArguments().iterator();
                while (iterator.hasNext()) {
                    final PathArgument currentArg = iterator.next();
                    currentArguments.add(currentArg);
                    final YangInstanceIdentifier yii = YangInstanceIdentifier.create(currentArguments);
                    delegate().merge(store, yii, codec.getDefaultNodeFor(yii));
                }
                super.merge(store, path, data);
            }

            @Override
            protected DOMDataWriteTransaction delegate() {
                return delegateTx;
            }
        };
    }

}
