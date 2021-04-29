/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless;

import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContentBuilder;

public class UserFunctionTests extends ScriptTestCase {
    public void testZeroArgumentUserFunction() {
        String source = "def twofive() { return 25; } twofive()";
        assertEquals(25, exec(source));
    }

    public void testIRBuilder() {
        String script = "def twofive() { return 25; } twofive()";
        Tuple<XContentBuilder, Tuple<XContentBuilder, XContentBuilder>> builders = ToXContentTests.phases(script);
        System.out.println("----------Semantic----------");
        System.out.println(builders.v1());
        System.out.println("----------IR----------");
        System.out.println(builders.v2().v1());
        System.out.println("----------ASM----------");
        System.out.println(builders.v2().v2());
    }
}
