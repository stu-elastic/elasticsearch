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
                "l.sort(this::myCompare);\n" +
                "if (l[0] == 100) { l.remove(l.size() - 1) ; l.sort((a, b) -> -1 * myCompare(a, b)) } \n"+
                "if (getSource().startsWith('sour')) { l.add(255); }\n" +
                "return l;";
        assertEquals(List.of(1, 49, 100, 255), exec(source));
        assertBytecodeExists(source, "public static &getSource()Ljava/lang/String");
        assertBytecodeExists(source, "public static &getMulti()I");
        assertBytecodeExists(source, "INVOKESTATIC org/elasticsearch/painless/PainlessScript$Script.&getMulti ()I");
        assertBytecodeExists(source, "public static &myCompare(II)I");
        assertBytecodeExists(source, "INVOKESTATIC org/elasticsearch/painless/PainlessScript$Script.&myCompare (II)I");
    }
    public void testUserFunctionCapture() {
        String source =
            "int myCompare(Object o, int x, int y) { return o != null ? -1 * ( x - y ) : ( x - y ) }\n" +
                //"int myCompare(int m, int x, int y) { return m * ( x - y ) }\n" +
                "List l = [1, 100, -100];\n" +
                //"int q = -1;\n" +
                "Object q = '';\n" +
                "l.sort((a, b) -> myCompare(q, a, b));\n" +
                "return l;";
        System.out.println(source);
        System.out.println(Debugger.toString(source));
        assertEquals(List.of(100, 1, -100), exec(source, Map.of("a", 1), false));
        //assertBytecodeExists(source, "public &myCompare(II)I");
        //assertBytecodeExists(source, "INVOKESTATIC org/elasticsearch/painless/PainlessScript$Script.&myCompare (II)I");
    }
}
