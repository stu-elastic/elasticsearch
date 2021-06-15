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

    public void testChainedUserMethods() {
        String source = "int myCompare(int a, int b) { getMulti() * (a - b) }\n" +
                        "int getMulti() { -1 }\n" +
                        "List l = [1, 100, -100];\n" +
                        "l.sort(this::myCompare);\n" +
                        "l;\n";
        assertEquals(List.of(100, 1, -100), exec(source, Map.of("a", 1), false));
        System.out.println(Debugger.toString(source));
    }


    public void testChainedUserMethodsLambda() {
        String source = "int myCompare(int a, int b) { getMulti() * (a - b) }\n" +
                        "int getMulti() { -1 }\n" +
                        "List l = [1, 100, -100];\n" +
                        "l.sort((a, b) -> myCompare(a, b));\n" +
                        "l;\n";
        assertEquals(List.of(100, 1, -100), exec(source, Map.of("a", 1), false));
        System.out.println(Debugger.toString(source));
    }

    public void testChainedUserMethodsDef() {
        String source = "int myCompare(int a, int b) { getMulti() * (a - b) }\n" +
                        "int getMulti() { -1 }\n" +
                        "def l = [1, 100, -100];\n" +
                        "l.sort(this::myCompare);\n" +
                        "l;\n";
        assertEquals(List.of(100, 1, -100), exec(source, Map.of("a", 1), false));
        System.out.println(Debugger.toString(source));
    }


    public void testChainedUserMethodsLambdaDef() {
        String source = "int myCompare(int a, int b) { getMulti() * (a - b) }\n" +
                        "int getMulti() { -1 }\n" +
                        "def l = [1, 100, -100];\n" +
                        "l.sort((a, b) -> myCompare(a, b));\n" +
                        "l;\n";
        assertEquals(List.of(100, 1, -100), exec(source, Map.of("a", 1), false));
        System.out.println(Debugger.toString(source));
    }

    public void testChainedUserMethodsLambdaCaptureDef() {
        String source = "int myCompare(int a, int b, int x, int m) { getMulti(m) * (a - b + x) }\n" +
                        "int getMulti(int m) { -1 * m }\n" +
                        "def l = [1, 100, -100];\n" +
                        "int cx = 100;\n" +
                        "int cm = 1;\n" +
                        "l.sort((a, b) -> myCompare(a, b, cx, cm));\n" +
                        "l;\n";
        assertEquals(List.of(100, 1, -100), exec(source, Map.of("a", 1), false));
        System.out.println(Debugger.toString(source));
    }

    public void testMethodReferenceInUserFunction() {
        String source = "int myCompare(int a, int b, String s) { " +
                        "   Map m = ['f': 5];" +
                        "   a - b + m.computeIfAbsent(s, this::getLength) " +
                        "}\n" +
                        "int getLength(String s) { s.length() }\n" +
                        "def l = [1, 0, -2];\n" +
                        "String s = 'g';\n" +
                        "l.sort((a, b) -> myCompare(a, b, s));\n" +
                        "l;\n";
        assertEquals(List.of(-2, 1, 0), exec(source, Map.of("a", 1), false));
        System.out.println(Debugger.toString(source));
    }

    public void testUserFunctionVirtual() {
        String source = "int myCompare(int x, int y) { return -1 * (x - y)  }\n" +
                        "return myCompare(100, 90);";
        System.out.println(source);
        System.out.println(Debugger.toString(source));
        assertEquals(-10, exec(source, Map.of("a", 1), false));
        assertBytecodeExists(source, "INVOKEVIRTUAL org/elasticsearch/painless/PainlessScript$Script.&myCompare (II)I");
    }

    public void testUserFunctionRef() {
        String source = "int myCompare(int x, int y) { return -1 * x - y  }\n" +
                        "List l = [1, 100, -100];\n" +
                        "l.sort(this::myCompare);\n" +
                        "return l;";
        System.out.println(source);
        System.out.println(Debugger.toString(source));
        assertEquals(List.of(100, 1, -100), exec(source, Map.of("a", 1), false));
        assertBytecodeExists(source, "public &myCompare(II)I");
        //assertBytecodeExists(source, "INVOKESTATIC org/elasticsearch/painless/PainlessScript$Script.&myCompare (II)I");
    }

    public void testUserFunctionRefEmpty() {
        String source = "int myCompare(int x, int y) { return -1 * x - y  }\n" +
                        "[].sort((a, b) -> myCompare(a, b));\n";
        System.out.println(source);
        System.out.println(Debugger.toString(source));
        assertNull(exec(source, Map.of("a", 1), false));
        assertBytecodeExists(source, "public &myCompare(II)I");
        //assertBytecodeExists(source, "INVOKESTATIC org/elasticsearch/painless/PainlessScript$Script.&myCompare (II)I");
    }

    public void testUserFunctionCallInLambda() {
        String source = "int myCompare(int x, int y) { -1 * ( x - y ) }\n" +
                        "List l = [1, 100, -100];\n" +
                        "l.sort((a, b) -> myCompare(a, b));\n" +
                        "return l;";
        System.out.println(source);
        System.out.println(Debugger.toString(source));
        assertEquals(List.of(100, 1, -100), exec(source, Map.of("a", 1), false));
        //assertBytecodeExists(source, "public &myCompare(II)I");
        //assertBytecodeExists(source, "INVOKESTATIC org/elasticsearch/painless/PainlessScript$Script.&myCompare (II)I");
    }

    public void testUserFunctionCapture() {
        String source = "int myCompare(Object o, int x, int y) { return o != null ? -1 * ( x - y ) : ( x - y ) }\n" +
                        "List l = [1, 100, -100];\n" +
                        "Object q = '';\n" +
                        "l.sort((a, b) -> myCompare(q, a, b));\n" +
                        "return l;";
        System.out.println(source);
        System.out.println(Debugger.toString(source));
        assertEquals(List.of(100, 1, -100), exec(source, Map.of("a", 1), false));
        //assertBytecodeExists(source, "public &myCompare(II)I");
        //assertBytecodeExists(source, "INVOKESTATIC org/elasticsearch/painless/PainlessScript$Script.&myCompare (II)I");
    }

    public void testCapture() {
        String source = "List l = [1, 100, -100];\n" +
                        "int q = -1;\n" +
                        "l.sort((a, b) -> q * ( a - b ));\n" +
                        "return l;";
        System.out.println(source);
        System.out.println(Debugger.toString(source));
        assertEquals(List.of(100, 1, -100), exec(source, Map.of("a", 1), false));
        assertBytecodeExists(source, "public &myCompare(II)I");
        assertBytecodeExists(source, "INVOKEVIRTUAL org/elasticsearch/painless/PainlessScript$Script.&myCompare (II)I");
    }
}
