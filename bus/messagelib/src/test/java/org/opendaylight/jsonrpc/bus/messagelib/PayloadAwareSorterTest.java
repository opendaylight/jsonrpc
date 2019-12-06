/*
 * Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.jsonrpc.bus.messagelib;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

/**
 * Test for {@link Util#payloadwareSorter(com.google.gson.JsonElement)}.
 *
 * @author <a href="mailto:richard.kosegi@gmail.com">Richard Kosegi</a>
 * @since Dec 6, 2019
 */
public class PayloadAwareSorterTest {
    public static class ObjectArg {
        int field;
    }

    public static class Service {
        public void test(int arg) {
            // NOOP
        }

        public void test(ObjectArg arg) {
            // NOOP
        }
    }

    @Test
    public void test() throws ReflectiveOperationException {
        Method primitive = Service.class.getDeclaredMethod("test", int.class);
        Method object = Service.class.getDeclaredMethod("test", ObjectArg.class);
        // object argument
        Comparator<Method> sorter = Util.payloadAwareSorter(new JsonObject());
        List<Method> list = Lists.<Method>newArrayList(primitive, object);
        Collections.sort(list, sorter);
        assertEquals(object, list.get(0));

        list = Lists.<Method>newArrayList(object, primitive);
        Collections.sort(list, sorter);
        assertEquals(object, list.get(0));

        // primitive argument
        sorter = Util.payloadAwareSorter(new JsonPrimitive(10));
        list = Lists.<Method>newArrayList(primitive, object);
        Collections.sort(list, sorter);
        assertEquals(primitive, list.get(0));

        list = Lists.<Method>newArrayList(object, primitive);
        Collections.sort(list, sorter);
        assertEquals(primitive, list.get(0));
    }
}
