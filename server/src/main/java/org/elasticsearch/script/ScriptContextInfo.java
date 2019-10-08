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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScriptContextInfo {
    public final String name;
    public final ScriptMethodInfo execute;
    public final List<ScriptMethodInfo> getters;

    private ScriptContextInfo(String name, ScriptMethodInfo execute,  List<ScriptMethodInfo> getters) {
        this.name = name;
        this.execute = execute;
        this.getters = getters;
    }

    public List<ScriptMethodInfo> methods() {
        ArrayList<ScriptMethodInfo> methods = new ArrayList<>();
        methods.add(this.execute);
        methods.addAll(this.getters);
        return Collections.unmodifiableList(methods);
    }

    static ScriptContextInfo fromContext(String name, Class<?> clazz) {
        return new ScriptContextInfo(name, ScriptMethodInfo.executeFromContext(clazz), ScriptMethodInfo.gettersFromContext(clazz));
    }

    public static class ScriptMethodInfo {
        public final String name, returnType;
        public final List<ParameterInfo> parameters;

        private ScriptMethodInfo(String name, String returnType, List<ParameterInfo> parameters) {
            this.name = name;
            this.returnType = returnType;
            this.parameters = Collections.unmodifiableList(parameters);
        }

        public static class ParameterInfo {
            public final String type, name;

            ParameterInfo(String type, String name) {
                this.type = type;
                this.name = name;
            }
        }

        static ScriptMethodInfo executeFromContext(Class<?> clazz) {
            Method execute = null;
            String name = "execute";

            // See ScriptContext.findMethod
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(name)) {
                    if (execute != null) {
                        throw new IllegalArgumentException("Cannot have multiple [" + name + "] methods on class [" + clazz.getName() + "]");
                    }
                    execute = method;
                }
            }
            if (execute == null) {
                throw new IllegalArgumentException("Could not find method [" + name + "] on class [" + clazz.getName() + "]");
            }

            Class<?> returnTypeClazz = execute.getReturnType();
            String returnType = returnTypeClazz.getTypeName();

            Class<?>[] parameterTypes = execute.getParameterTypes();
            List<ParameterInfo> parameters = new ArrayList<>();
            if (parameterTypes.length > 0) {
                // TODO(stu): ensure empty/no PARAMETERS if parameterTypes.length == 0?
                String parametersFieldName = "PARAMETERS";

                // See ScriptClassInfo.readArgumentNamesConstant
                Field parameterNamesField;
                try {
                    parameterNamesField = clazz.getField(parametersFieldName);
                } catch (NoSuchFieldException e) {
                    throw new IllegalArgumentException("Could not find field [" + parametersFieldName + "] on instance class [" +
                        clazz.getName() + "] but method [" + name + "] has [" + parameterTypes.length + "] parameters");
                }
                if (!parameterNamesField.getType().equals(String[].class)) {
                    throw new IllegalArgumentException("Expected a constant [String[] PARAMETERS] on instance class [" +
                        clazz.getName() + "] for method [" + name + "] with [" + parameterTypes.length + "] parameters, found [" +
                        parameterNamesField.getType().getTypeName() + "]");
                }

                String[] argumentNames;
                try {
                    argumentNames = (String[]) parameterNamesField.get(null);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IllegalArgumentException("Error trying to read [" + clazz.getName() + "#ARGUMENTS]", e);
                }

                if (argumentNames.length != parameterTypes.length) {
                    throw new IllegalArgumentException("Expected argument names [" + argumentNames.length +
                        "] to have the same arity [" + parameterTypes.length + "] for method [" + name +
                        "] of class [" + clazz.getName() + "]");
                }

                for (int i = 0; i < argumentNames.length; i++) {
                    parameters.add(new ParameterInfo(parameterTypes[i].getTypeName(), argumentNames[i]));
                }
            }
            return new ScriptMethodInfo(name, returnType, parameters);
        }

        static List<ScriptMethodInfo> gettersFromContext(Class<?> clazz) {
            ArrayList<ScriptMethodInfo> getters = new ArrayList<>();
            for (java.lang.reflect.Method m : clazz.getMethods()) {
                if (!m.isDefault() &&
                    m.getName().startsWith("get") &&
                    !m.getName().equals("getClass") &&
                    !Modifier.isStatic(m.getModifiers()) &&
                    m.getParameters().length == 0) {
                    getters.add(new ScriptMethodInfo(m.getName(), m.getReturnType().getTypeName(), new ArrayList<>()));
                }
            }
            return getters;
        }
    }
}
