/*
 * Copyright (c) 2020 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.model;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * API to publish into OSGi registry so that other components can consume it.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Jul 2, 2020
 */
public interface GovernanceProvider extends Supplier<Optional<RemoteGovernance>> {

}
