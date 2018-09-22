/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

/**
 * Tests for {@link Util} class.
 *
 * @author <a href="mailto:rkosegi@brocade.com">Richard Kosegi</a>
 *
 */
public class UtilTest {
    @Test(expected = RuntimeException.class)
    public void testInvalidIntStore() {
        Util.int2store(3);
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidStringStore() {
        Util.store2int("Config");
    }

    @Test
    public void testStoreMapping() {
        assertEquals(LogicalDatastoreType.CONFIGURATION, Util.int2store(Util.store2int("config")));
        assertEquals(LogicalDatastoreType.OPERATIONAL, Util.int2store(Util.store2int("operational")));
    }

    @Test
    public void ensureUtilPrivateCtor() {
        assertTrue(TestUtils.assertPrivateConstructor(Util.class));
    }

    @Test
    public void ensureYangInstanceIdentifierDeserializerPrivateCtor() {
        assertTrue(TestUtils.assertPrivateConstructor(YangInstanceIdentifierDeserializer.class));
    }
}
