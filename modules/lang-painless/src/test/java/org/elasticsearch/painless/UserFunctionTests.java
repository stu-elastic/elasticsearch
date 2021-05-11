/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless;

import java.util.List;
import java.util.Map;

public class UserFunctionTests extends ScriptTestCase {
    public void testZeroArgumentUserFunction() {
        String source = "def twofive() { return 25; } twofive()";
        assertEquals(25, exec(source));
    }

    public void testUserFunctionDefCallRef() {
        String source =
            "String getSource() { 'source'; }\n" +
            "int myCompare(int a, int b) { getMulti() * Integer.compare(a, b) }\n" +
                "int getMulti() { return -1 }\n" +
                "def l = [1, 100, -100];\n" +
                "if (myCompare(10, 50) > 0) { l.add(50 + getMulti()) }\n" +
                //"l.sort(this::myCompare);\n" +
                //"l.sort((a, b) -> -1 * params.get('a'));\n" +
                //"if (l[0] == 100) { l.remove(l.size() - 1) ; l.sort((a, b) -> -1 * myCompare(a, b)) } \n"+
                "if (getSource().startsWith('sour')) { l.add(255); }\n" +
                "return l;";
        System.out.println(Debugger.toString(source));
        // assertEquals(List.of(1, 49, 100, 255), exec(source, Map.of("a", 1), false));
        assertEquals(List.of(1, 100, -100, 49, 255), exec(source, Map.of("a", 1), false));
        assertBytecodeExists(source, "public static &getSource()Ljava/lang/String");
        assertBytecodeExists(source, "public static &getMulti()I");
        assertBytecodeExists(source, "INVOKESTATIC org/elasticsearch/painless/PainlessScript$Script.&getMulti ()I");
        assertBytecodeExists(source, "public static &myCompare(II)I");
        assertBytecodeExists(source, "INVOKESTATIC org/elasticsearch/painless/PainlessScript$Script.&myCompare (II)I");
    }
}
