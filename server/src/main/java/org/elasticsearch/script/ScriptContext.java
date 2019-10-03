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

/**
 * The information necessary to compile and run a script.
 *
 * A {@link ScriptContext} contains the information related to a single use case and the interfaces
 * and methods necessary for a {@link ScriptEngine} to implement.
 * <p>
 * There are at least two (and optionally a third) related classes which must be defined.
 * <p>
 * The <i>InstanceType</i> is a class which users of the script api call to execute a script. It
 * may be stateful. Instances of
 * the <i>InstanceType</i> may be executed multiple times by a caller with different arguments. This
 * class must have an abstract method named {@code execute} which {@link ScriptEngine} implementations
 * will define.
 * <p>
 * The <i>FactoryType</i> is a factory class returned by the {@link ScriptService} when compiling
 * a script. This class must be stateless so it is cacheable by the {@link ScriptService}. It must
 * have one of the following:
 * <ul>
 *     <li>An abstract method named {@code newInstance} which returns an instance of <i>InstanceType</i></li>
 *     <li>An abstract method named {@code newFactory} which returns an instance of <i>StatefulFactoryType</i></li>
 * </ul>
 * <p>
 * The <i>StatefulFactoryType</i> is an optional class which allows a stateful factory from the
 * stateless factory type required by the {@link ScriptService}. If defined, the <i>StatefulFactoryType</i>
 * must have a method named {@code newInstance} which returns an instance of <i>InstanceType</i>.
 * <p>
 * Both the <i>FactoryType</i> and <i>StatefulFactoryType</i> may have abstract methods to indicate
 * whether a variable is used in a script. These method should return a {@code boolean} and their name
 * should start with {@code needs}, followed by the variable name, with the first letter uppercased.
 * For example, to check if a variable {@code doc} is used, a method {@code boolean needsDoc()} should be added.
 * If the variable name starts with an underscore, for example, {@code _score}, the needs method would
 * be {@code boolean needs_score()}.
 */
public final class ScriptContext<FactoryType> {

    /** A unique identifier for this context. */
    public final String name;

    /** A factory class for constructing script or stateful factory instances. */
    public final Class<FactoryType> factoryClazz;

    /** A factory class for construct script instances. */
    public final Class<?> statefulFactoryClazz;

    /** A class that is an instance of a script. */
    public final Class<?> instanceClazz;

    /** The execute method of instanceClazz */
    public final ExecuteInfo execute;

    /** Construct a context with the related instance and compiled classes. */
    public ScriptContext(String name, Class<FactoryType> factoryClazz) {
        this.name = name;
        this.factoryClazz = factoryClazz;
        Method newInstanceMethod = findMethod("FactoryType", factoryClazz, "newInstance");
        Method newFactoryMethod = findMethod("FactoryType", factoryClazz, "newFactory");
        if (newFactoryMethod != null) {
            assert newInstanceMethod == null;
            statefulFactoryClazz = newFactoryMethod.getReturnType();
            newInstanceMethod = findMethod("StatefulFactoryType", statefulFactoryClazz, "newInstance");
            if (newInstanceMethod == null) {
                throw new IllegalArgumentException("Could not find method newInstance StatefulFactoryType class ["
                    + statefulFactoryClazz.getName() + "] for script context [" + name + "]");
            }
        } else if (newInstanceMethod != null) {
            assert newFactoryMethod == null;
            statefulFactoryClazz = null;
        } else {
            throw new IllegalArgumentException("Could not find method newInstance or method newFactory on FactoryType class ["
                + factoryClazz.getName() + "] for script context [" + name + "]");
        }
        instanceClazz = newInstanceMethod.getReturnType();
        execute = new ExecuteInfo(instanceClazz);
    }

    /** Returns a method with the given name, or throws an exception if multiple are found. */
    private Method findMethod(String type, Class<?> clazz, String methodName) {
        Method foundMethod = null;
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                if (foundMethod != null) {
                    throw new IllegalArgumentException("Cannot have multiple " + methodName + " methods on " + type + " class ["
                        + clazz.getName() + "] for script context [" + name + "]");
                }
                foundMethod = method;
            }
        }
        return foundMethod;
    }

    public class ExecuteInfo {
        public final String name, returnType;
        public final List<ParameterInfo> parameters;

        ExecuteInfo(Class<?> instanceClazz) {
            name = "execute";
            Method method = findMethod("Object", instanceClazz, name);
            if (method == null) {
                throw new IllegalArgumentException("Could not find method [" + name + "] on instance class [" + instanceClazz.getName() +"]");
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
                        instanceClazz.getName() +"] but method [" + name + "] has [" + parameterTypes.length + "] parameters");
                }
                if (!parameterNamesField.getType().equals(String[].class)) {
                    throw new IllegalArgumentException("Expected needs a constant [String[] PARAMETERS] on instance class [" +
                        instanceClazz.getName() +"] for method [" + name + "] with [" + parameterTypes.length + "] parameters");
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

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(this.name);
            builder.append(' ');
            builder.append(this.returnType);
            builder.append(' ');
            builder.append('(');
            boolean hadParam = false;
            for (ParameterInfo param: this.parameters) {
                if (hadParam) {
                    builder.append(", ");
                } else {
                    hadParam = true;
                }
                builder.append(param.type);
                builder.append(' ');
                builder.append(param.name);
            }
            builder.append(')');
            return builder.toString();
        }

        public class ParameterInfo {
            public final String type, name;
            ParameterInfo(String type, String name) {
                this.type = type;
                this.name = name;
            }
        }
    }
}
