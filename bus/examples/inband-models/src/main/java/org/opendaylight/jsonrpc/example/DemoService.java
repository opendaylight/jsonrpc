/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.example;

import com.google.gson.JsonObject;

import org.opendaylight.jsonrpc.model.InbandModelsService;

public interface DemoService extends InbandModelsService {
    JsonObject factorial(JsonObject input);
}
