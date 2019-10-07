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
import java.util.ArrayList;
import java.util.List;

public class ScriptExecuteInfo {
    public final String name, returnType;
    public final List<ParameterInfo> parameters;

    ScriptExecuteInfo(Class<?> instanceClazz, Method method) {
        name = "execute";
        if (method == null) {
            throw new IllegalArgumentException("Could not find method [" + name + "] on instance class [" + instanceClazz.getName() + "]");
        }
        Class<?> returnTypeClazz = method.getReturnType();
        this.returnType = returnTypeClazz.getTypeName();

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length > 0) {
            String parametersFieldName = "PARAMETERS";

            // See ScriptClassInfo.readArgumentNamesConstant
            Field parameterNamesField;
            try {
                parameterNamesField = instanceClazz.getField(parametersFieldName);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Could not find field [" + parametersFieldName + "] on instance class [" +
                    instanceClazz.getName() + "] but method [" + name + "] has [" + parameterTypes.length + "] parameters");
            }
            if (!parameterNamesField.getType().equals(String[].class)) {
                throw new IllegalArgumentException("Expected needs a constant [String[] PARAMETERS] on instance class [" +
                    instanceClazz.getName() + "] for method [" + name + "] with [" + parameterTypes.length + "] parameters");
            }

            String[] argumentNames;
            try {
                argumentNames = (String[]) parameterNamesField.get(null);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new IllegalArgumentException("Error trying to read [" + instanceClazz.getName() + "#ARGUMENTS]", e);
            }

            if (argumentNames.length != parameterTypes.length) {
                throw new IllegalArgumentException("Expected argument names [" + argumentNames.length +
                    "] to have the same arity as parameters [" + parameterTypes.length + "] for method [" + name +
                    "] of instance class " + instanceClazz.getName());
            }

            parameters = new ArrayList<ParameterInfo>(argumentNames.length);
            for (int i = 0; i < argumentNames.length; i++) {
                parameters.add(new ParameterInfo(parameterTypes[i].getTypeName(), argumentNames[i]));
            }
        } else {
            parameters = new ArrayList<ParameterInfo>();
        }
    }

    public static class ParameterInfo {
        public final String type, name;

        ParameterInfo(String type, String name) {
            this.type = type;
            this.name = name;
        }
    }
}
