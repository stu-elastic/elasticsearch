/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless.toxcontent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

public class JavaToXContent {
    public static void visitMethod(Method method, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("class", method.getDeclaringClass().getSimpleName());
        builder.field("name", method.getName());
        builder.field("parameterTypes", Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.toList()));
        builder.field("modifiers", Modifier.toString(method.getModifiers()));
        builder.field("return", method.getReturnType().getSimpleName());
        Type[] exceptionTypes = method.getExceptionTypes();
        if (exceptionTypes.length > 0) {
            builder.field("exceptions", Arrays.stream(exceptionTypes).map(Type::getTypeName).collect(Collectors.toList()));
        }
    }

    public static void visitMethodHandle(MethodHandle handle, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("type", "MethodHandle");
        MethodType type = handle.type();
        builder.field("parameter", type.parameterList().stream().map(Class::getSimpleName).collect(Collectors.toList()));
        builder.field("return", type.returnType().getSimpleName());
        builder.endObject();
    }
}
