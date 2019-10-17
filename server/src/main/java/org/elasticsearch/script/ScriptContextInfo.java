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

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.elasticsearch.common.xcontent.ConstructingObjectParser.constructorArg;
import static org.elasticsearch.common.xcontent.XContentParserUtils.ensureExpectedToken;

public class ScriptContextInfo {
    public final String name;
    public final ScriptMethodInfo execute;
    public final List<ScriptMethodInfo> getters;

    public ScriptContextInfo(String name, ScriptMethodInfo execute,  List<ScriptMethodInfo> getters) {
        this.name = Objects.requireNonNull(name);
        this.execute = Objects.requireNonNull(execute);
        this.getters = Objects.requireNonNull(getters);
    }

    public ScriptContextInfo(StreamInput in) throws IOException {
        this.name = in.readString();
        this.execute = new ScriptMethodInfo(in);
        int numGetters = in.readInt();
        List<ScriptMethodInfo> getters = new ArrayList<>(numGetters);
        for (int i = 0; i < numGetters; i++) {
            getters.add(new ScriptMethodInfo(in));
        }
        this.getters = Collections.unmodifiableList(getters);
    }

    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(name);
        execute.writeTo(out);
        out.writeInt(getters.size());
        for (ScriptMethodInfo getter: getters) {
            getter.writeTo(out);
        }
    }

    public List<ScriptMethodInfo> methods() {
        ArrayList<ScriptMethodInfo> methods = new ArrayList<>();
        methods.add(this.execute);
        methods.addAll(this.getters);
        return Collections.unmodifiableList(methods);
    }

    ScriptContextInfo(String name, Class<?> clazz) {
        this.name = name;
        this.execute = ScriptMethodInfo.executeFromContext(clazz);
        this.getters = ScriptMethodInfo.gettersFromContext(clazz);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScriptContextInfo that = (ScriptContextInfo) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(execute, that.execute) &&
            Objects.equals(getters, that.getters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, execute, getters);
    }

    public static class ScriptMethodInfo {
        public final String name, returnType;
        public final List<ParameterInfo> parameters;

        public static String NAME_FIELD = "name";
        public static String RETURN_TYPE_FIELD = "return_type";
        public static String PARAMETERS_FIELD = "params";

        ScriptMethodInfo(String name, String returnType, List<ParameterInfo> parameters) {
            this.name = name;
            this.returnType = returnType;
            this.parameters = Collections.unmodifiableList(parameters);
        }

        private ScriptMethodInfo(StreamInput in) throws IOException {
            this.name = in.readString();
            this.returnType = in.readString();
            int numParameters = in.readInt();
            ArrayList<ParameterInfo> parameters = new ArrayList<>(numParameters);
            for (int i = 0; i < numParameters; i++) {
                parameters.add(new ParameterInfo(in));
            }
            this.parameters = Collections.unmodifiableList(parameters);
        }

        void writeTo(StreamOutput out) throws IOException {
            out.writeString(name);
            out.writeString(returnType);
            out.writeInt(parameters.size());
            for (ParameterInfo parameter: parameters) {
                parameter.writeTo(out);
            }
        }

        @SuppressWarnings("unchecked")
        private static ConstructingObjectParser<ScriptMethodInfo,Void> PARSER =
            new ConstructingObjectParser<>("method", true,
                (m, name) -> new ScriptMethodInfo((String) m[0], (String) m[1], (ArrayList<ParameterInfo>) m[2])
            );

        static {
            PARSER.declareString(constructorArg(), new ParseField(NAME_FIELD));
            PARSER.declareString(constructorArg(), new ParseField(RETURN_TYPE_FIELD));
            PARSER.declareObjectArray(constructorArg(),
                (parser, ctx) -> ParameterInfo.PARSER.apply(parser, ctx),
                new ParseField(PARAMETERS_FIELD));
        }

        public static ScriptMethodInfo fromXContent(XContentParser parser) throws IOException {
            return PARSER.parse(parser, null);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScriptMethodInfo that = (ScriptMethodInfo) o;
            return Objects.equals(name, that.name) &&
                Objects.equals(returnType, that.returnType) &&
                Objects.equals(parameters, that.parameters);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, returnType, parameters);
        }

        public static class ParameterInfo implements ToXContentObject {
            public final String type, name;

            public static String TYPE_FIELD = "type";
            public static String NAME_FIELD = "name";

            ParameterInfo(String type, String name) {
                this.type = type;
                this.name = name;
            }

            ParameterInfo(StreamInput in) throws IOException {
                this.type = in.readString();
                this.name = in.readString();
            }

            void writeTo(StreamOutput out) throws IOException {
                out.writeString(type);
                out.writeString(name);
            }

            private static ConstructingObjectParser<ParameterInfo,Void> PARSER =
                new ConstructingObjectParser<>("parameters", true,
                    (p) -> new ParameterInfo((String)p[0], (String)p[1])
            );

            static {
                PARSER.declareString(constructorArg(), new ParseField(TYPE_FIELD));
                PARSER.declareString(constructorArg(), new ParseField(NAME_FIELD));
            }

            public static ParameterInfo fromXContent(XContentParser parser) throws IOException {
                return PARSER.parse(parser, null);
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
                return builder.startObject().field(TYPE_FIELD, this.type).field(NAME_FIELD, this.name).endObject();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                ParameterInfo that = (ParameterInfo) o;
                return Objects.equals(type, that.type) &&
                    Objects.equals(name, that.name);
            }

            @Override
            public int hashCode() {
                return Objects.hash(type, name);
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
