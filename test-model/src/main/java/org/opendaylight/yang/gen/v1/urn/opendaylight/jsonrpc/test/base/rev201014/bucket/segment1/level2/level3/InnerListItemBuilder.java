/*
* Copyright (c) 2017 Brocade Communications Systems, Inc. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.yang.gen.v1.urn.opendaylight.jsonrpc.test.base.rev201014.bucket.segment1.level2.level3;

public class InnerListItemBuilder {
    public static InnerList.Item getDefaultInstance(String defaultValue) {
        try {
            return new InnerList.Item(Short.parseShort(defaultValue));
        } catch (NumberFormatException e) {
            return new InnerList.Item(defaultValue);
        }
    }
}
