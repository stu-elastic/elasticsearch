/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless.toxcontent;

import org.elasticsearch.painless.FunctionRef;
import org.elasticsearch.painless.lookup.PainlessCast;
import org.elasticsearch.painless.lookup.PainlessClassBinding;
import org.elasticsearch.painless.lookup.PainlessConstructor;
import org.elasticsearch.painless.lookup.PainlessField;
import org.elasticsearch.painless.lookup.PainlessInstanceBinding;
import org.elasticsearch.painless.lookup.PainlessMethod;
import org.elasticsearch.painless.spi.annotation.CompileTimeOnlyAnnotation;
import org.elasticsearch.painless.spi.annotation.DeprecatedAnnotation;
import org.elasticsearch.painless.spi.annotation.InjectConstantAnnotation;
import org.elasticsearch.painless.spi.annotation.NoImportAnnotation;
import org.elasticsearch.painless.spi.annotation.NonDeterministicAnnotation;
import org.elasticsearch.painless.symbol.Decorator.Decoration;
import org.elasticsearch.painless.symbol.Decorations.TargetType;
import org.elasticsearch.painless.symbol.Decorations.ValueType;
import org.elasticsearch.painless.symbol.Decorations.StaticType;
import org.elasticsearch.painless.symbol.Decorations.PartialCanonicalTypeName;
import org.elasticsearch.painless.symbol.Decorations.ExpressionPainlessCast;
import org.elasticsearch.painless.symbol.Decorations.SemanticVariable;
import org.elasticsearch.painless.symbol.Decorations.IterablePainlessMethod;
import org.elasticsearch.painless.symbol.Decorations.UnaryType;
import org.elasticsearch.painless.symbol.Decorations.BinaryType;
import org.elasticsearch.painless.symbol.Decorations.ShiftType;
import org.elasticsearch.painless.symbol.Decorations.ComparisonType;
import org.elasticsearch.painless.symbol.Decorations.CompoundType;
import org.elasticsearch.painless.symbol.Decorations.UpcastPainlessCast;
import org.elasticsearch.painless.symbol.Decorations.DowncastPainlessCast;
import org.elasticsearch.painless.symbol.Decorations.StandardPainlessField;
import org.elasticsearch.painless.symbol.Decorations.StandardPainlessConstructor;
import org.elasticsearch.painless.symbol.Decorations.StandardPainlessMethod;
import org.elasticsearch.painless.symbol.Decorations.GetterPainlessMethod;
import org.elasticsearch.painless.symbol.Decorations.SetterPainlessMethod;
import org.elasticsearch.painless.symbol.Decorations.StandardConstant;
import org.elasticsearch.painless.symbol.Decorations.StandardLocalFunction;
import org.elasticsearch.painless.symbol.Decorations.StandardPainlessClassBinding;
import org.elasticsearch.painless.symbol.Decorations.StandardPainlessInstanceBinding;
import org.elasticsearch.painless.symbol.Decorations.MethodNameDecoration;
import org.elasticsearch.painless.symbol.Decorations.ReturnType;
import org.elasticsearch.painless.symbol.Decorations.TypeParameters;
import org.elasticsearch.painless.symbol.Decorations.ParameterNames;
import org.elasticsearch.painless.symbol.Decorations.ReferenceDecoration;
import org.elasticsearch.painless.symbol.Decorations.EncodingDecoration;
import org.elasticsearch.painless.symbol.Decorations.CapturesDecoration;
import org.elasticsearch.painless.symbol.Decorations.InstanceType;
import org.elasticsearch.painless.symbol.Decorations.AccessDepth;
import org.elasticsearch.painless.symbol.Decorations.IRNodeDecoration;
import org.elasticsearch.painless.symbol.Decorations.Converter;
import org.elasticsearch.painless.symbol.FunctionTable;
import org.elasticsearch.painless.symbol.SemanticScope;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serialize user tree decorations from org.elasticsearch.painless.symbol.Decorations
 */
public class DecorationToXContent {
    static final class Fields {
        static final String DECORATION = "decoration";
        static final String TYPE = "type";
        static final String CAST = "cast";
        static final String METHOD = "method";
    }

    public static void toXContent(TargetType targetType, XContentBuilderWrapper builder) {
        start(targetType, builder);
        builder.field(Fields.TYPE, targetType.getTargetType().getSimpleName());
        builder.endObject();
    }

    public static void toXContent(ValueType valueType, XContentBuilderWrapper builder) {
        start(valueType, builder);
        builder.field(Fields.TYPE, valueType.getValueType().getSimpleName());
        builder.endObject();
    }

    public static void toXContent(StaticType staticType, XContentBuilderWrapper builder) {
        start(staticType, builder);
        builder.field(Fields.TYPE, staticType.getStaticType().getSimpleName());
        builder.endObject();
    }

    public static void toXContent(PartialCanonicalTypeName partialCanonicalTypeName, XContentBuilderWrapper builder) {
        start(partialCanonicalTypeName, builder);
        builder.field(Fields.TYPE, partialCanonicalTypeName.getPartialCanonicalTypeName());
        builder.endObject();
    }

    public static void toXContent(ExpressionPainlessCast expressionPainlessCast, XContentBuilderWrapper builder) {
        start(expressionPainlessCast, builder);
        builder.field(Fields.CAST);
        toXContent(expressionPainlessCast.getExpressionPainlessCast(), builder);
        builder.endObject();
    }

    public static void toXContent(SemanticVariable semanticVariable, XContentBuilderWrapper builder) {
        start(semanticVariable, builder);
        builder.field("variable");
        toXContent(semanticVariable.getSemanticVariable(), builder);
        builder.endObject();
    }

    public static void toXContent(IterablePainlessMethod iterablePainlessMethod, XContentBuilderWrapper builder) {
        start(iterablePainlessMethod, builder);
        builder.field(Fields.METHOD);
        toXContent(iterablePainlessMethod.getIterablePainlessMethod(), builder);
        builder.endObject();
    }

    public static void toXContent(UnaryType unaryType, XContentBuilderWrapper builder) {
        start(unaryType, builder);
        builder.field(Fields.TYPE, unaryType.getUnaryType().getSimpleName());
        builder.endObject();
    }

    public static void toXContent(BinaryType binaryType, XContentBuilderWrapper builder) {
        start(binaryType, builder);
        builder.field(Fields.TYPE, binaryType.getBinaryType().getSimpleName());
        builder.endObject();
    }

    public static void toXContent(ShiftType shiftType, XContentBuilderWrapper builder) {
        start(shiftType, builder);
        builder.field(Fields.TYPE, shiftType.getShiftType().getSimpleName());
        builder.endObject();
    }

    public static void toXContent(ComparisonType comparisonType, XContentBuilderWrapper builder) {
        start(comparisonType, builder);
        builder.field(Fields.TYPE, comparisonType.getComparisonType().getSimpleName());
        builder.endObject();
    }

    public static void toXContent(CompoundType compoundType, XContentBuilderWrapper builder) {
        start(compoundType, builder);
        builder.field(Fields.TYPE, compoundType.getCompoundType().getSimpleName());
        builder.endObject();
    }

    public static void toXContent(UpcastPainlessCast upcastPainlessCast, XContentBuilderWrapper builder) {
        start(upcastPainlessCast, builder);
        builder.field(Fields.CAST);
        toXContent(upcastPainlessCast.getUpcastPainlessCast(), builder);
        builder.endObject();
    }

    public static void toXContent(DowncastPainlessCast downcastPainlessCast, XContentBuilderWrapper builder) {
        start(downcastPainlessCast, builder);
        builder.field(Fields.CAST);
        toXContent(downcastPainlessCast.getDowncastPainlessCast(), builder);
        builder.endObject();
    }

    public static void toXContent(StandardPainlessField standardPainlessField, XContentBuilderWrapper builder) {
        start(standardPainlessField, builder);
        builder.field("field");
        toXContent(standardPainlessField.getStandardPainlessField(), builder);
        builder.endObject();
    }

    public static void toXContent(StandardPainlessConstructor standardPainlessConstructor, XContentBuilderWrapper builder) {
        start(standardPainlessConstructor, builder);
        builder.field("constructor");
        toXContent(standardPainlessConstructor.getStandardPainlessConstructor(), builder);
        builder.endObject();
    }

    public static void toXContent(StandardPainlessMethod standardPainlessMethod, XContentBuilderWrapper builder) {
        start(standardPainlessMethod, builder);
        builder.field(Fields.METHOD);
        toXContent(standardPainlessMethod.getStandardPainlessMethod(), builder);
        builder.endObject();
    }

    public static void toXContent(GetterPainlessMethod getterPainlessMethod, XContentBuilderWrapper builder) {
        start(getterPainlessMethod, builder);
        builder.field(Fields.METHOD);
        toXContent(getterPainlessMethod.getGetterPainlessMethod(), builder);
        builder.endObject();
    }

    public static void toXContent(SetterPainlessMethod setterPainlessMethod, XContentBuilderWrapper builder) {
        start(setterPainlessMethod, builder);
        builder.field(Fields.METHOD);
        toXContent(setterPainlessMethod.getSetterPainlessMethod(), builder);
        builder.endObject();
    }

    public static void toXContent(StandardConstant standardConstant, XContentBuilderWrapper builder) {
        start(standardConstant, builder);
        builder.startObject("constant");
        builder.field(Fields.TYPE, standardConstant.getStandardConstant().getClass().getSimpleName());
        builder.field("value", standardConstant.getStandardConstant());
        builder.endObject();
        builder.endObject();
    }

    public static void toXContent(StandardLocalFunction standardLocalFunction, XContentBuilderWrapper builder) {
        start(standardLocalFunction, builder);
        builder.field("function");
        toXContent(standardLocalFunction.getLocalFunction(), builder);
        builder.endObject();
    }

    public static void toXContent(StandardPainlessClassBinding standardPainlessClassBinding, XContentBuilderWrapper builder) {
        start(standardPainlessClassBinding, builder);
        builder.field("PainlessClassBinding");
        toXContent(standardPainlessClassBinding.getPainlessClassBinding(), builder);
        builder.endObject();
    }

    public static void toXContent(StandardPainlessInstanceBinding standardPainlessInstanceBinding, XContentBuilderWrapper builder) {
        start(standardPainlessInstanceBinding, builder);
        builder.field("PainlessInstanceBinding");
        toXContent(standardPainlessInstanceBinding.getPainlessInstanceBinding(), builder);
        builder.endObject();
    }

    public static void toXContent(MethodNameDecoration methodNameDecoration, XContentBuilderWrapper builder) {
        start(methodNameDecoration, builder);
        builder.field("methodName", methodNameDecoration.getMethodName());
        builder.endObject();
    }

    public static void toXContent(ReturnType returnType, XContentBuilderWrapper builder) {
        start(returnType, builder);
        builder.field("returnType", returnType.getReturnType().getSimpleName());
        builder.endObject();
    }

    public static void toXContent(TypeParameters typeParameters, XContentBuilderWrapper builder) {
        start(typeParameters, builder);
        if (typeParameters.getTypeParameters().isEmpty() == false) {
            builder.field("typeParameters", classNames(typeParameters.getTypeParameters()));
        }
        builder.endObject();
    }

    public static void toXContent(ParameterNames parameterNames, XContentBuilderWrapper builder) {
        start(parameterNames, builder);
        if (parameterNames.getParameterNames().isEmpty() == false) {
            builder.field("parameterNames", parameterNames.getParameterNames());
        }
        builder.endObject();
    }

    public static void toXContent(FunctionRef ref, XContentBuilderWrapper builder) {
        builder.field("interfaceMethodName", ref.interfaceMethodName);

        builder.field("interfaceMethodType");
        toXContent(ref.interfaceMethodType, builder);

        builder.field("delegateClassName", ref.delegateClassName);
        builder.field("isDelegateInterface", ref.isDelegateInterface);
        builder.field("isDelegateAugmented", ref.isDelegateAugmented);
        builder.field("delegateInvokeType", ref.delegateInvokeType);
        builder.field("delegateMethodName", ref.delegateMethodName);

        builder.field("delegateMethodType");
        toXContent(ref.delegateMethodType, builder);

        if (ref.delegateInjections.length > 0) {
            builder.startArray("delegateInjections");
            for (Object obj : ref.delegateInjections) {
                builder.startObject();
                builder.field("type", obj.getClass().getSimpleName());
                builder.field("value", obj);
                builder.endObject();
            }
            builder.endArray();
        }

        builder.field("factoryMethodType");
        toXContent(ref.factoryMethodType, builder);
    }

    public static void toXContent(ReferenceDecoration referenceDecoration, XContentBuilderWrapper builder) {
        start(referenceDecoration, builder);
        toXContent(referenceDecoration.getReference(), builder);
        builder.endObject();
    }

    public static void toXContent(EncodingDecoration encodingDecoration, XContentBuilderWrapper builder) {
        start(encodingDecoration, builder);
        builder.field("encoding", encodingDecoration.getEncoding());
        builder.endObject();
    }

    public static void toXContent(CapturesDecoration capturesDecoration, XContentBuilderWrapper builder) {
        start(capturesDecoration, builder);
        if (capturesDecoration.getCaptures().isEmpty() == false) {
            builder.startArray("captures");
            for (SemanticScope.Variable capture : capturesDecoration.getCaptures()) {
                toXContent(capture, builder);
            }
            builder.endArray();
        }
        builder.endObject();
    }

    public static void toXContent(InstanceType instanceType, XContentBuilderWrapper builder) {
        start(instanceType, builder);
        builder.field("instanceType", instanceType.getInstanceType().getSimpleName());
        builder.endObject();
    }

    public static void toXContent(AccessDepth accessDepth, XContentBuilderWrapper builder) {
        start(accessDepth, builder);
        builder.field("depth", accessDepth.getAccessDepth());
        builder.endObject();
    }

    public static void toXContent(IRNodeDecoration irNodeDecoration, XContentBuilderWrapper builder) {
        start(irNodeDecoration, builder);
        builder.field("irNode");
        IRNodeToXContent.toXContent(irNodeDecoration.getIRNode(), builder);
        builder.endObject();
    }

    public static void toXContent(Converter converter, XContentBuilderWrapper builder) {
        start(converter, builder);
        builder.field("converter");
        toXContent(converter.getConverter(), builder);
        builder.endObject();
    }

    public static void toXContent(Decoration decoration, XContentBuilderWrapper builder) {
        if  (decoration instanceof TargetType) {
            toXContent((TargetType) decoration, builder);
        } else if (decoration instanceof ValueType) {
            toXContent((ValueType) decoration, builder);
        } else if (decoration instanceof StaticType) {
            toXContent((StaticType) decoration, builder);
        } else if (decoration instanceof PartialCanonicalTypeName) {
            toXContent((PartialCanonicalTypeName) decoration, builder);
        } else if (decoration instanceof ExpressionPainlessCast) {
            toXContent((ExpressionPainlessCast) decoration, builder);
        } else if (decoration instanceof SemanticVariable) {
            toXContent((SemanticVariable) decoration, builder);
        } else if (decoration instanceof IterablePainlessMethod) {
            toXContent((IterablePainlessMethod) decoration, builder);
        } else if (decoration instanceof UnaryType) {
            toXContent((UnaryType) decoration, builder);
        } else if (decoration instanceof BinaryType) {
            toXContent((BinaryType) decoration, builder);
        } else if (decoration instanceof ShiftType) {
            toXContent((ShiftType) decoration, builder);
        } else if (decoration instanceof ComparisonType) {
            toXContent((ComparisonType) decoration, builder);
        } else if (decoration instanceof CompoundType) {
            toXContent((CompoundType) decoration, builder);
        } else if (decoration instanceof UpcastPainlessCast) {
            toXContent((UpcastPainlessCast) decoration, builder);
        } else if (decoration instanceof DowncastPainlessCast) {
            toXContent((DowncastPainlessCast) decoration, builder);
        } else if (decoration instanceof StandardPainlessField) {
            toXContent((StandardPainlessField) decoration, builder);
        } else if (decoration instanceof StandardPainlessConstructor) {
            toXContent((StandardPainlessConstructor) decoration, builder);
        } else if (decoration instanceof StandardPainlessMethod) {
            toXContent((StandardPainlessMethod) decoration, builder);
        } else if (decoration instanceof GetterPainlessMethod) {
            toXContent((GetterPainlessMethod) decoration, builder);
        } else if (decoration instanceof SetterPainlessMethod) {
            toXContent((SetterPainlessMethod) decoration, builder);
        } else if (decoration instanceof StandardConstant) {
            toXContent((StandardConstant) decoration, builder);
        } else if (decoration instanceof StandardLocalFunction) {
            toXContent((StandardLocalFunction) decoration, builder);
        } else if (decoration instanceof StandardPainlessClassBinding) {
            toXContent((StandardPainlessClassBinding) decoration, builder);
        } else if (decoration instanceof StandardPainlessInstanceBinding) {
            toXContent((StandardPainlessInstanceBinding) decoration, builder);
        } else if (decoration instanceof MethodNameDecoration) {
            toXContent((MethodNameDecoration) decoration, builder);
        } else if (decoration instanceof ReturnType) {
            toXContent((ReturnType) decoration, builder);
        } else if (decoration instanceof TypeParameters) {
            toXContent((TypeParameters) decoration, builder);
        } else if (decoration instanceof ParameterNames) {
            toXContent((ParameterNames) decoration, builder);
        } else if (decoration instanceof ReferenceDecoration) {
            toXContent((ReferenceDecoration) decoration, builder);
        } else if (decoration instanceof EncodingDecoration) {
            toXContent((EncodingDecoration) decoration, builder);
        } else if (decoration instanceof CapturesDecoration) {
            toXContent((CapturesDecoration) decoration, builder);
        } else if (decoration instanceof InstanceType) {
            toXContent((InstanceType) decoration, builder);
        } else if (decoration instanceof AccessDepth) {
            toXContent((AccessDepth) decoration, builder);
        } else if (decoration instanceof IRNodeDecoration) {
            toXContent((IRNodeDecoration) decoration, builder);
        } else if (decoration instanceof Converter) {
            toXContent((Converter) decoration, builder);
        } else {
            builder.startObject();
            builder.field(Fields.DECORATION, decoration.getClass().getSimpleName());
            builder.endObject();
        }
    }

    // lookup
    public static void toXContent(PainlessCast painlessCast, XContentBuilderWrapper builder) {
        builder.startObject();
        if (painlessCast.originalType != null) {
            builder.field("originalType", painlessCast.originalType.getSimpleName());
        }
        if (painlessCast.targetType != null) {
            builder.field("targetType", painlessCast.targetType.getSimpleName());
        }

        builder.field("explicitCast", painlessCast.explicitCast);

        if (painlessCast.unboxOriginalType != null) {
            builder.field("unboxOriginalType", painlessCast.unboxOriginalType.getSimpleName());
        }
        if (painlessCast.unboxTargetType != null) {
            builder.field("unboxTargetType", painlessCast.unboxTargetType.getSimpleName());
        }
        if (painlessCast.boxOriginalType != null) {
            builder.field("boxOriginalType", painlessCast.boxOriginalType.getSimpleName());
        }
        builder.endObject();
    }

    public static void toXContent(PainlessMethod method, XContentBuilderWrapper builder) {
        builder.startObject();
        if (method.javaMethod != null) {
            builder.field("javaMethod");
            toXContent(method.methodType, builder);
        }
        if (method.targetClass != null) {
            builder.field("targetClass", method.targetClass.getSimpleName());
        }
        if (method.returnType != null) {
            builder.field("returnType", method.returnType.getSimpleName());
        }
        if (method.typeParameters != null && method.typeParameters.isEmpty() == false) {
            builder.field("typeParameters", classNames(method.typeParameters));
        }
        if (method.methodHandle != null) {
            builder.field("methodHandle");
            toXContent(method.methodHandle.type(), builder);
        }
        // ignoring methodType as that's handled under methodHandle
        annotationsToXContent(method.annotations, builder);
        builder.endObject();
    }

    public static void toXContent(FunctionTable.LocalFunction localFunction, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("functionName", localFunction.getFunctionName());
        builder.field("returnType", localFunction.getReturnType().getSimpleName());
        if (localFunction.getTypeParameters().isEmpty() == false) {
            builder.field("typeParameters", classNames(localFunction.getTypeParameters()));
        }
        builder.field("isInternal", localFunction.isInternal());
        builder.field("isStatic", localFunction.isStatic());
        builder.field("methodType");
        toXContent(localFunction.getMethodType(), builder);
        builder.endObject();
    }

    public static void toXContent(PainlessClassBinding binding, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("javaConstructor");
        toXContent(binding.javaConstructor, builder);

        builder.field("javaMethod");
        toXContent(binding.javaMethod, builder);
        builder.field("returnType", binding.returnType.getSimpleName());
        if (binding.typeParameters.isEmpty() == false) {
            builder.field("typeParameters", classNames(binding.typeParameters));
        }
        annotationsToXContent(binding.annotations, builder);
        builder.endObject();
    }

    public static void toXContent(PainlessInstanceBinding binding, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("targetInstance", binding.targetInstance.getClass().getSimpleName());

        builder.field("javaMethod");
        toXContent(binding.javaMethod, builder);
        builder.field("returnType", binding.returnType.getSimpleName());
        if (binding.typeParameters.isEmpty() == false) {
            builder.field("typeParameters", classNames(binding.typeParameters));
        }
        annotationsToXContent(binding.annotations, builder);
        builder.endObject();
    }

    public static void toXContent(PainlessField field, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("javaField");
        toXContent(field.javaField, builder);
        builder.field("typeParameter", field.typeParameter.getSimpleName());
        builder.field("getterMethodHandle");
        toXContent(field.getterMethodHandle.type(), builder);
        builder.field("setterMethodHandle");
        if (field.setterMethodHandle != null) {
            toXContent(field.setterMethodHandle.type(), builder);
        }
        builder.endObject();
    }

    public static void toXContent(PainlessConstructor constructor, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("javaConstructor");
        toXContent(constructor.javaConstructor, builder);
        if (constructor.typeParameters.isEmpty() == false) {
            builder.field("typeParameters", classNames(constructor.typeParameters));
        }
        builder.field("methodHandle");
        toXContent(constructor.methodHandle.type(), builder);
        builder.endObject();
    }

    // symbol
    public static void toXContent(SemanticScope.Variable variable, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field(Fields.TYPE, variable.getType());
        builder.field("name", variable.getName());
        builder.field("isFinal", variable.isFinal());
        builder.endObject();
    }

    // annotations
    private static void annotationsToXContent(Map<Class<?>, Object> annotations, XContentBuilderWrapper builder) {
        if (annotations == null || annotations.isEmpty()) {
            return;
        }
        builder.startArray("annotations");
        for (Class<?> key : annotations.keySet().stream().sorted().collect(Collectors.toList())) {
            annotationToXContent(annotations.get(key), builder);
        }
        builder.endArray();
    }

    private static void annotationToXContent(Object annotation, XContentBuilderWrapper builder) {
        if (annotation instanceof CompileTimeOnlyAnnotation) {
            builder.value(CompileTimeOnlyAnnotation.NAME);
        } else if (annotation instanceof DeprecatedAnnotation) {
            builder.startObject();
            builder.field("name", DeprecatedAnnotation.NAME);
            builder.field("message", ((DeprecatedAnnotation) annotation).getMessage());
            builder.endObject();
        } else if (annotation instanceof InjectConstantAnnotation) {
            builder.startObject();
            builder.field("name", InjectConstantAnnotation.NAME);
            builder.field("message", ((InjectConstantAnnotation) annotation).injects);
            builder.endObject();
        } else if (annotation instanceof NoImportAnnotation) {
            builder.value(NoImportAnnotation.NAME);
        } else if (annotation instanceof NonDeterministicAnnotation) {
            builder.value(NonDeterministicAnnotation.NAME);
        } else {
            builder.value(annotation.toString());
        }
    }

    // asm
    private static void toXContent(org.objectweb.asm.commons.Method asmMethod, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("name", asmMethod.getName());
        builder.field("descriptor", asmMethod.getDescriptor());
        builder.field("returnType", asmMethod.getReturnType().getClassName());
        builder.field("argumentTypes", Arrays.stream(asmMethod.getArgumentTypes()).map(Type::getClassName));
        builder.endObject();
    }

    // java.lang.invoke
    private static void toXContent(MethodType methodType, XContentBuilderWrapper builder) {
        builder.startObject();
        List<Class<?>> parameters = methodType.parameterList();
        if (parameters.isEmpty() == false) {
            builder.field("parameters", classNames(parameters));
        }
        builder.field("return", methodType.returnType().getSimpleName());
        builder.endObject();
    }

    // java.lang.reflect
    private static void toXContent(Field field, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("name", field.getName());
        builder.field("type", field.getType().getSimpleName());
        builder.field("modifiers", Modifier.toString(field.getModifiers()));
        builder.endObject();
    }

    private static void toXContent(Method method, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("name", method.getName());
        builder.field("parameters", classNames(method.getParameterTypes()));
        builder.field("return", method.getReturnType().getSimpleName());
        Class<?>[] exceptions = method.getExceptionTypes();
        if (exceptions.length > 0) {
            builder.field("exceptions", classNames(exceptions));
        }
        builder.field("modifiers", Modifier.toString(method.getModifiers()));
        builder.endObject();
    }

    private static void toXContent(Constructor<?> constructor, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("name", constructor.getName());
        if (constructor.getParameterTypes().length > 0) {
            builder.field("parameterTypes", classNames(constructor.getParameterTypes()));
        }
        if (constructor.getExceptionTypes().length > 0) {
            builder.field("exceptionTypes", classNames(constructor.getExceptionTypes()));
        }
        builder.field("modifiers", Modifier.toString(constructor.getModifiers()));
        builder.endObject();
    }

    // helpers
    private static void start(Decoration decoration, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field(Fields.DECORATION, decoration.getClass().getSimpleName());
    }

    private static List<String> classNames(Class<?>[] classes) {
        return Arrays.stream(classes).map(Class::getSimpleName).collect(Collectors.toList());
    }

    private static List<String> classNames(List<Class<?>> classes) {
        return classes.stream().map(Class::getSimpleName).collect(Collectors.toList());
    }
}
