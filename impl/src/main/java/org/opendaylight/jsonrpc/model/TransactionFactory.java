/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.util.function.Supplier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;

/**
 * Factory abstraction to provide {@link DOMDataWriteTransaction} instances.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public interface TransactionFactory extends Supplier<DOMDataWriteTransaction> {
}
