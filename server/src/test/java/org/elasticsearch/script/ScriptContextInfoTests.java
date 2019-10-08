/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.script;

import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.test.ESTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptContextInfoTests extends ESTestCase {
    public interface MinimalContext {
        public void execute();
    }

    public void testMinimalContext() {
        String name = "minimal_context";
        ScriptContextInfo info = ScriptContextInfo.fromContext(name, MinimalContext.class);
        assertEquals(name, info.name);
        assertEquals("execute", info.execute.name);
        assertEquals("void", info.execute.returnType);
        assertEquals(0, info.execute.parameters.size());
        assertEquals(0, info.getters.size());
    }

    public static class PrimitiveContext {
        public int execute(boolean foo, long bar, short baz, float qux) {return 0;}
        public static final String[] PARAMETERS = {"foo", "bar", "baz", "qux"};
        public byte getByte() {return 0x00;}
        public char getChar() {return 'a';}
    }

    public void testPrimitiveContext() {
        String name = "primitive_context";
        ScriptContextInfo info = ScriptContextInfo.fromContext(name, PrimitiveContext.class);
        assertEquals(name, info.name);
        assertEquals("execute", info.execute.name);
        assertEquals("int", info.execute.returnType);
        assertEquals(4, info.execute.parameters.size());
        List<Tuple<String, String>> eparams = new ArrayList<>();
        eparams.add(new Tuple<>("boolean", "foo"));
        eparams.add(new Tuple<>("long", "bar"));
        eparams.add(new Tuple<>("short", "baz"));
        eparams.add(new Tuple<>("float", "qux"));
        for (int i=0; i < info.execute.parameters.size(); i++) {
            assertEquals(eparams.get(i).v1(), info.execute.parameters.get(i).type);
            assertEquals(eparams.get(i).v2(), info.execute.parameters.get(i).name);
        }
        assertEquals(2, info.getters.size());
        HashMap<String,String> getters = new HashMap(Map.of("getByte","byte", "getChar","char"));
        for (ScriptContextInfo.ScriptMethodInfo getter: info.getters) {
            assertEquals(0, getter.parameters.size());
            String returnType = getters.remove(getter.name);
            assertNotNull(returnType);
            assertEquals(returnType, getter.returnType);
        }
        assertEquals(0, getters.size());
    }


    public static class CustomType0 {}
    public static class CustomType1 {}
    public static class CustomType2 {}

    public static class CustomTypeContext {
        public CustomType0 execute(CustomType1 custom1, CustomType2 custom2) {return new CustomType0();}
        public static final String[] PARAMETERS = {"custom1", "custom2"};
        public CustomType1 getCustom1() {return new CustomType1();}
        public CustomType2 getCustom2() {return new CustomType2();}
    }

    public void testCustomTypeContext() {
        String ct = "org.elasticsearch.script.ScriptContextInfoTests$CustomType";
        String ct0 = ct + 0;
        String ct1 = ct + 1;
        String ct2 = ct + 2;
        String name = "custom_type_context";
        ScriptContextInfo info = ScriptContextInfo.fromContext(name, CustomTypeContext.class);
        assertEquals(name, info.name);
        assertEquals("execute", info.execute.name);
        assertEquals(ct0, info.execute.returnType);
        assertEquals(2, info.execute.parameters.size());
        List<Tuple<String, String>> eparams = new ArrayList<>();
        eparams.add(new Tuple<>(ct1, "custom1"));
        eparams.add(new Tuple<>(ct2, "custom2"));
        for (int i=0; i < info.execute.parameters.size(); i++) {
            assertEquals(eparams.get(i).v1(), info.execute.parameters.get(i).type);
            assertEquals(eparams.get(i).v2(), info.execute.parameters.get(i).name);
        }
        assertEquals(2, info.getters.size());
        HashMap<String,String> getters = new HashMap(Map.of("getCustom1",ct1, "getCustom2",ct2));
        for (ScriptContextInfo.ScriptMethodInfo getter: info.getters) {
            assertEquals(0, getter.parameters.size());
            String returnType = getters.remove(getter.name);
            assertNotNull(returnType);
            assertEquals(returnType, getter.returnType);
        }
        assertEquals(0, getters.size());

        HashMap<String,String> methods = new HashMap(Map.of("getCustom1",ct1, "getCustom2",ct2, "execute",ct0));
        for (ScriptContextInfo.ScriptMethodInfo method: info.methods()) {
            String returnType = methods.remove(method.name);
            assertNotNull(returnType);
            assertEquals(returnType, method.returnType);
        }
        assertEquals(0, methods.size());
    }

    public static class TwoExecute {
        public void execute(int foo) {}
        public boolean execute(boolean foo) {return foo;}
        public static final String[] PARAMETERS = {"foo"};
    }

    public void testTwoExecute() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () ->
            ScriptContextInfo.fromContext("two_execute", TwoExecute.class));
        assertEquals("Cannot have multiple [execute] methods on class [" + TwoExecute.class.getName() + "]", e.getMessage());
    }

    public static class NoExecute {}

    public void testNoExecute() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () ->
            ScriptContextInfo.fromContext("no_execute", NoExecute.class));
        assertEquals("Could not find method [execute] on class [" + NoExecute.class.getName() + "]", e.getMessage());
    }

    public static class NoParametersField {
        public void execute(int foo) {}
    }

    public void testNoParametersField() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () ->
            ScriptContextInfo.fromContext("no_parameters_field", NoParametersField.class));
        assertEquals("Could not find field [PARAMETERS] on instance class [" + NoParametersField.class.getName() +
            "] but method [execute] has [1] parameters", e.getMessage());
    }

    public static class BadParametersFieldType {
        public void execute(int foo) {}
        public static final int[] PARAMETERS = {1};
    }

    public void testBadParametersFieldType() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () ->
            ScriptContextInfo.fromContext("bad_parameters_field_type", BadParametersFieldType.class));
        assertEquals("Expected a constant [String[] PARAMETERS] on instance class [" + BadParametersFieldType.class.getName() +
            "] for method [execute] with [1] parameters, found [int[]]", e.getMessage());
    }

    public static class WrongNumberOfParameters {
        public void execute(int foo) {}
        public static final String[] PARAMETERS = {"foo", "bar"};
    }

    public void testWrongNumberOfParameters() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () ->
            ScriptContextInfo.fromContext("wrong_number_of_parameters", WrongNumberOfParameters.class));
        assertEquals("Expected argument names [2] to have the same arity [1] for method [execute] of class ["
            + WrongNumberOfParameters.class.getName() + "]", e.getMessage());
    }

    public interface Default {
        public default int getDefault() {return 1;}
        public boolean getNonDefault1();
    }

    public static class GetterConditional implements Default {
        public void execute() {}
        public boolean getNonDefault1() {return true;}
        public float getNonDefault2() {return 0.1f;}
        public static long getStatic() {return 2L;}
        public char getChar(char ch) { return ch;}
    }

    public void testGetterConditional() {
        List<ScriptContextInfo.ScriptMethodInfo> getters =
            ScriptContextInfo.fromContext("getter_conditional", GetterConditional.class).getters;
        assertEquals(2, getters.size());
        HashMap<String,String> methods = new HashMap(Map.of("getNonDefault1","boolean", "getNonDefault2","float"));
        for (ScriptContextInfo.ScriptMethodInfo method: getters) {
            String returnType = methods.remove(method.name);
            assertNotNull(returnType);
            assertEquals(returnType, method.returnType);
        }
        assertEquals(0, methods.size());
    }
}
