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
public class UserDecorationToXContent {

    static final class Fields {
        static final String DECORATION = "decoration";
        static final String TYPE = "type";
        static final String CAST = "cast";
        static final String METHOD = "method";
    }

    public static void visitTargetType(TargetType targetType, XContentBuilderWrapper builder) {
        start(targetType, builder);
        builder.field(Fields.TYPE, targetType.getTargetType().getSimpleName());
        builder.endObject();
    }

    public static void visitValueType(ValueType valueType, XContentBuilderWrapper builder) {
        start(valueType, builder);
        builder.field(Fields.TYPE, valueType.getValueType().getSimpleName());
        builder.endObject();
    }

    public static void visitStaticType(StaticType staticType, XContentBuilderWrapper builder) {
        start(staticType, builder);
        builder.field(Fields.TYPE, staticType.getStaticType().getSimpleName());
        builder.endObject();
    }

    public static void visitPartialCanonicalTypeName(PartialCanonicalTypeName partialCanonicalTypeName, XContentBuilderWrapper builder) {
        start(partialCanonicalTypeName, builder);
        builder.field(Fields.TYPE, partialCanonicalTypeName.getPartialCanonicalTypeName());
        builder.endObject();
    }

    public static void visitExpressionPainlessCast(ExpressionPainlessCast expressionPainlessCast, XContentBuilderWrapper builder) {
        start(expressionPainlessCast, builder);
        builder.field(Fields.CAST);
        visitPainlessCast(expressionPainlessCast.getExpressionPainlessCast(), builder);
        builder.endObject();
    }

    /*
    // TODO(stu): add when GlobalMember annotation is added
    public static void visitGlobalMember(GlobalMember globalMember, XContentBuilderWrapper builder) {
        start(globalMember, builder);
        builder.field("isLocalMember", globalMember.isLocalMember());
        builder.endObject();
    }
     */

    public static void visitSemanticVariable(SemanticVariable semanticVariable, XContentBuilderWrapper builder) {
        start(semanticVariable, builder);
        builder.field("variable");
        visitVariable(semanticVariable.getSemanticVariable(), builder);
        builder.endObject();
    }

    public static void visitIterablePainlessMethod(IterablePainlessMethod iterablePainlessMethod, XContentBuilderWrapper builder) {
        start(iterablePainlessMethod, builder);
        builder.field(Fields.METHOD);
        visitPainlessMethod(iterablePainlessMethod.getIterablePainlessMethod(), builder);
        builder.endObject();
    }

    public static void visitUnaryType(UnaryType unaryType, XContentBuilderWrapper builder) {
        start(unaryType, builder);
        builder.field(Fields.TYPE, unaryType.getUnaryType().getSimpleName());
        builder.endObject();
    }

    public static void visitBinaryType(BinaryType binaryType, XContentBuilderWrapper builder) {
        start(binaryType, builder);
        builder.field(Fields.TYPE, binaryType.getBinaryType().getSimpleName());
        builder.endObject();
    }

    public static void visitShiftType(ShiftType shiftType, XContentBuilderWrapper builder) {
        start(shiftType, builder);
        builder.field(Fields.TYPE, shiftType.getShiftType().getSimpleName());
        builder.endObject();
    }

    public static void visitComparisonType(ComparisonType comparisonType, XContentBuilderWrapper builder) {
        start(comparisonType, builder);
        builder.field(Fields.TYPE, comparisonType.getComparisonType().getSimpleName());
        builder.endObject();
    }

    public static void visitCompoundType(CompoundType compoundType, XContentBuilderWrapper builder) {
        start(compoundType, builder);
        builder.field(Fields.TYPE, compoundType.getCompoundType().getSimpleName());
        builder.endObject();
    }

    public static void visitUpcastPainlessCast(UpcastPainlessCast upcastPainlessCast, XContentBuilderWrapper builder) {
        start(upcastPainlessCast, builder);
        builder.field(Fields.CAST);
        visitPainlessCast(upcastPainlessCast.getUpcastPainlessCast(), builder);
        builder.endObject();
    }

    public static void visitDowncastPainlessCast(DowncastPainlessCast downcastPainlessCast, XContentBuilderWrapper builder) {
        start(downcastPainlessCast, builder);
        builder.field(Fields.CAST);
        visitPainlessCast(downcastPainlessCast.getDowncastPainlessCast(), builder);
        builder.endObject();
    }

    public static void visitStandardPainlessField(StandardPainlessField standardPainlessField, XContentBuilderWrapper builder) {
        start(standardPainlessField, builder);
        builder.field("field");
        visitPainlessField(standardPainlessField.getStandardPainlessField(), builder);
        builder.endObject();
    }

    public static void visitStandardPainlessConstructor(StandardPainlessConstructor standardPainlessConstructor, XContentBuilderWrapper builder) {
        start(standardPainlessConstructor, builder);
        builder.field("constructor");
        visitPainlessConstructor(standardPainlessConstructor.getStandardPainlessConstructor(), builder);
        builder.endObject();
    }

    public static void visitStandardPainlessMethod(StandardPainlessMethod standardPainlessMethod, XContentBuilderWrapper builder) {
        start(standardPainlessMethod, builder);
        builder.field(Fields.METHOD);
        visitPainlessMethod(standardPainlessMethod.getStandardPainlessMethod(), builder);
        builder.endObject();
    }

    public static void visitGetterPainlessMethod(GetterPainlessMethod getterPainlessMethod, XContentBuilderWrapper builder) {
        start(getterPainlessMethod, builder);
        builder.field(Fields.METHOD);
        visitPainlessMethod(getterPainlessMethod.getGetterPainlessMethod(), builder);
        builder.endObject();
    }

    public static void visitSetterPainlessMethod(SetterPainlessMethod setterPainlessMethod, XContentBuilderWrapper builder) {
        start(setterPainlessMethod, builder);
        builder.field(Fields.METHOD);
        visitPainlessMethod(setterPainlessMethod.getSetterPainlessMethod(), builder);
        builder.endObject();
    }

    public static void visitStandardConstant(StandardConstant standardConstant, XContentBuilderWrapper builder) {
        start(standardConstant, builder);
        builder.startObject("constant");
        builder.field(Fields.TYPE, standardConstant.getStandardConstant().getClass().getSimpleName());
        builder.field("value", standardConstant.getStandardConstant());
        builder.endObject();
        builder.endObject();
    }

    public static void visitStandardLocalFunction(StandardLocalFunction standardLocalFunction, XContentBuilderWrapper builder) {
        start(standardLocalFunction, builder);
        builder.field("function");
        visitLocalFunction(standardLocalFunction.getLocalFunction(), builder);
        builder.endObject();
    }

    public static void visitStandardPainlessClassBinding(StandardPainlessClassBinding standardPainlessClassBinding, XContentBuilderWrapper builder) {
        start(standardPainlessClassBinding, builder);
        builder.field("PainlessClassBinding");
        visitPainlessClassBinding(standardPainlessClassBinding.getPainlessClassBinding(), builder);
        builder.endObject();
    }

    public static void visitStandardPainlessInstanceBinding(StandardPainlessInstanceBinding standardPainlessInstanceBinding, XContentBuilderWrapper builder) {
        start(standardPainlessInstanceBinding, builder);
        builder.field("PainlessInstanceBinding");
        visitPainlessInstanceBinding(standardPainlessInstanceBinding.getPainlessInstanceBinding(), builder);
        builder.endObject();
    }

    public static void visitMethodNameDecoration(MethodNameDecoration methodNameDecoration, XContentBuilderWrapper builder) {
        start(methodNameDecoration, builder);
        builder.field("methodName", methodNameDecoration.getMethodName());
        builder.endObject();
    }

    public static void visitReturnType(ReturnType returnType, XContentBuilderWrapper builder) {
        start(returnType, builder);
        builder.field("returnType", returnType.getReturnType().getSimpleName());
        builder.endObject();
    }

    public static void visitTypeParameters(TypeParameters typeParameters, XContentBuilderWrapper builder) {
        start(typeParameters, builder);
        if (typeParameters.getTypeParameters().isEmpty() == false) {
            builder.field("typeParameters", classNames(typeParameters.getTypeParameters()));
        }
        builder.endObject();
    }

    public static void visitParameterNames(ParameterNames parameterNames, XContentBuilderWrapper builder) {
        start(parameterNames, builder);
        if (parameterNames.getParameterNames().isEmpty() == false) {
            builder.field("parameterNames", parameterNames.getParameterNames());
        }
        builder.endObject();
    }

    public static void visitFunctionRef(FunctionRef ref, XContentBuilderWrapper builder) {
        builder.field("interfaceMethodName", ref.interfaceMethodName);

        builder.field("interfaceMethodType");
        visitMethodType(ref.interfaceMethodType, builder);

        builder.field("delegateClassName", ref.delegateClassName);
        builder.field("isDelegateInterface", ref.isDelegateInterface);
        builder.field("isDelegateAugmented", ref.isDelegateAugmented);
        builder.field("delegateInvokeType", ref.delegateInvokeType);
        builder.field("delegateMethodName", ref.delegateMethodName);

        builder.field("delegateMethodType");
        visitMethodType(ref.delegateMethodType, builder);

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
        visitMethodType(ref.factoryMethodType, builder);
    }

    public static void visitReferenceDecoration(ReferenceDecoration referenceDecoration, XContentBuilderWrapper builder) {
        start(referenceDecoration, builder);
        visitFunctionRef(referenceDecoration.getReference(), builder);
        builder.endObject();
    }

    public static void visitEncodingDecoration(EncodingDecoration encodingDecoration, XContentBuilderWrapper builder) {
        start(encodingDecoration, builder);
        builder.field("encoding", encodingDecoration.getEncoding());
        builder.endObject();
    }

    public static void visitCapturesDecoration(CapturesDecoration capturesDecoration, XContentBuilderWrapper builder) {
        start(capturesDecoration, builder);
        if (capturesDecoration.getCaptures().isEmpty() == false) {
            builder.startArray("captures");
            for (SemanticScope.Variable capture : capturesDecoration.getCaptures()) {
                visitVariable(capture, builder);
            }
            builder.endArray();
        }
        builder.endObject();
    }

    public static void visitInstanceType(InstanceType instanceType, XContentBuilderWrapper builder) {
        start(instanceType, builder);
        builder.field("instanceType", instanceType.getInstanceType().getSimpleName());
        builder.endObject();
    }

    public static void visitAccessDepth(AccessDepth accessDepth, XContentBuilderWrapper builder) {
        start(accessDepth, builder);
        builder.field("depth", accessDepth.getAccessDepth());
        builder.endObject();
    }

    public static void visitIRNodeDecoration(IRNodeDecoration irNodeDecoration, XContentBuilderWrapper builder) {
        start(irNodeDecoration, builder);
        builder.field("irNode");
        IRNodeToXContent.visitIR(irNodeDecoration.getIRNode(), builder);
        builder.endObject();
    }

    public static void visitConverter(Converter converter, XContentBuilderWrapper builder) {
        start(converter, builder);
        builder.field("converter");
        visitLocalFunction(converter.getConverter(), builder);
        builder.endObject();
    }

    public static void visitDecoration(Decoration decoration, XContentBuilderWrapper builder) {
        if  (decoration instanceof TargetType) {
            visitTargetType((TargetType) decoration, builder);
        } else if (decoration instanceof ValueType) {
            visitValueType((ValueType) decoration, builder);
        } else if (decoration instanceof StaticType) {
            visitStaticType((StaticType) decoration, builder);
        } else if (decoration instanceof PartialCanonicalTypeName) {
            visitPartialCanonicalTypeName((PartialCanonicalTypeName) decoration, builder);
        } else if (decoration instanceof ExpressionPainlessCast) {
            visitExpressionPainlessCast((ExpressionPainlessCast) decoration, builder);
        /*
        // TODO(stu): add when GlobalMember annotation is added
        } else if (decoration instanceof GlobalMember) {
            visitGlobalMember((GlobalMember) decoration, builder);
         */
        } else if (decoration instanceof SemanticVariable) {
            visitSemanticVariable((SemanticVariable) decoration, builder);
        } else if (decoration instanceof IterablePainlessMethod) {
            visitIterablePainlessMethod((IterablePainlessMethod) decoration, builder);
        } else if (decoration instanceof UnaryType) {
            visitUnaryType((UnaryType) decoration, builder);
        } else if (decoration instanceof BinaryType) {
            visitBinaryType((BinaryType) decoration, builder);
        } else if (decoration instanceof ShiftType) {
            visitShiftType((ShiftType) decoration, builder);
        } else if (decoration instanceof ComparisonType) {
            visitComparisonType((ComparisonType) decoration, builder);
        } else if (decoration instanceof CompoundType) {
            visitCompoundType((CompoundType) decoration, builder);
        } else if (decoration instanceof UpcastPainlessCast) {
            visitUpcastPainlessCast((UpcastPainlessCast) decoration, builder);
        } else if (decoration instanceof DowncastPainlessCast) {
            visitDowncastPainlessCast((DowncastPainlessCast) decoration, builder);
        } else if (decoration instanceof StandardPainlessField) {
            visitStandardPainlessField((StandardPainlessField) decoration, builder);
        } else if (decoration instanceof StandardPainlessConstructor) {
            visitStandardPainlessConstructor((StandardPainlessConstructor) decoration, builder);
        } else if (decoration instanceof StandardPainlessMethod) {
            visitStandardPainlessMethod((StandardPainlessMethod) decoration, builder);
        } else if (decoration instanceof GetterPainlessMethod) {
            visitGetterPainlessMethod((GetterPainlessMethod) decoration, builder);
        } else if (decoration instanceof SetterPainlessMethod) {
            visitSetterPainlessMethod((SetterPainlessMethod) decoration, builder);
        } else if (decoration instanceof StandardConstant) {
            visitStandardConstant((StandardConstant) decoration, builder);
        } else if (decoration instanceof StandardLocalFunction) {
            visitStandardLocalFunction((StandardLocalFunction) decoration, builder);
        } else if (decoration instanceof StandardPainlessClassBinding) {
            visitStandardPainlessClassBinding((StandardPainlessClassBinding) decoration, builder);
        } else if (decoration instanceof StandardPainlessInstanceBinding) {
            visitStandardPainlessInstanceBinding((StandardPainlessInstanceBinding) decoration, builder);
        } else if (decoration instanceof MethodNameDecoration) {
            visitMethodNameDecoration((MethodNameDecoration) decoration, builder);
        } else if (decoration instanceof ReturnType) {
            visitReturnType((ReturnType) decoration, builder);
        } else if (decoration instanceof TypeParameters) {
            visitTypeParameters((TypeParameters) decoration, builder);
        } else if (decoration instanceof ParameterNames) {
            visitParameterNames((ParameterNames) decoration, builder);
        } else if (decoration instanceof ReferenceDecoration) {
            visitReferenceDecoration((ReferenceDecoration) decoration, builder);
        } else if (decoration instanceof EncodingDecoration) {
            visitEncodingDecoration((EncodingDecoration) decoration, builder);
        } else if (decoration instanceof CapturesDecoration) {
            visitCapturesDecoration((CapturesDecoration) decoration, builder);
        } else if (decoration instanceof InstanceType) {
            visitInstanceType((InstanceType) decoration, builder);
        } else if (decoration instanceof AccessDepth) {
            visitAccessDepth((AccessDepth) decoration, builder);
        } else if (decoration instanceof IRNodeDecoration) {
            visitIRNodeDecoration((IRNodeDecoration) decoration, builder);
        } else if (decoration instanceof Converter) {
            visitConverter((Converter) decoration, builder);
        } else {
            builder.startObject();
            builder.field(Fields.DECORATION, decoration.getClass().getSimpleName());
            builder.endObject();
        }
    }

    // lookup
    public static void visitPainlessCast(PainlessCast painlessCast, XContentBuilderWrapper builder) {
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

    public static void visitPainlessMethod(PainlessMethod method, XContentBuilderWrapper builder) {
        builder.startObject();
        if (method.javaMethod != null) {
            builder.field("javaMethod");
            visitMethodType(method.methodType, builder);
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
            visitMethodType(method.methodHandle.type(), builder);
        }
        // ignoring methodType as that's handled under methodHandle
        visitAnnotationsFragment(method.annotations, builder);
        builder.endObject();
    }

    public static void visitLocalFunction(FunctionTable.LocalFunction localFunction, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("functionName", localFunction.getFunctionName());
        builder.field("returnType", localFunction.getReturnType().getSimpleName());
        if (localFunction.getTypeParameters().isEmpty() == false) {
            builder.field("typeParameters", classNames(localFunction.getTypeParameters()));
        }
        builder.field("isInternal", localFunction.isInternal());
        builder.field("isStatic", localFunction.isStatic());
        builder.field("methodType");
        visitMethodType(localFunction.getMethodType(), builder);
        builder.endObject();
    }

    public static void visitPainlessClassBinding(PainlessClassBinding binding, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("javaConstructor");
        visitConstructor(binding.javaConstructor, builder);

        builder.field("javaMethod");
        visitMethod(binding.javaMethod, builder);
        builder.field("returnType", binding.returnType.getSimpleName());
        if (binding.typeParameters.isEmpty() == false) {
            builder.field("typeParameters", classNames(binding.typeParameters));
        }
        visitAnnotationsFragment(binding.annotations, builder);
        builder.endObject();
    }

    public static void visitPainlessInstanceBinding(PainlessInstanceBinding binding, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("targetInstance", binding.targetInstance.getClass().getSimpleName());

        builder.field("javaMethod");
        visitMethod(binding.javaMethod, builder);
        builder.field("returnType", binding.returnType.getSimpleName());
        if (binding.typeParameters.isEmpty() == false) {
            builder.field("typeParameters", classNames(binding.typeParameters));
        }
        visitAnnotationsFragment(binding.annotations, builder);
        builder.endObject();
    }

    public static void visitPainlessField(PainlessField field, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("javaField");
        visitField(field.javaField, builder);
        builder.field("typeParameter", field.typeParameter.getSimpleName());
        builder.field("getterMethodHandle");
        visitMethodType(field.getterMethodHandle.type(), builder);
        builder.field("setterMethodHandle");
        if (field.setterMethodHandle != null) {
            visitMethodType(field.setterMethodHandle.type(), builder);
        }
        builder.endObject();
    }

    public static void visitPainlessConstructor(PainlessConstructor constructor, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("javaConstructor");
        visitConstructor(constructor.javaConstructor, builder);
        if (constructor.typeParameters.isEmpty() == false) {
            builder.field("typeParameters", classNames(constructor.typeParameters));
        }
        builder.field("methodHandle");
        visitMethodType(constructor.methodHandle.type(), builder);
        builder.endObject();
    }

    // symbol
    public static void visitVariable(SemanticScope.Variable variable, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field(Fields.TYPE, variable.getType());
        builder.field("name", variable.getName());
        builder.field("isFinal", variable.isFinal());
        builder.endObject();
    }

    // annotations
    private static void visitAnnotationsFragment(Map<Class<?>, Object> annotations, XContentBuilderWrapper builder) {
        if (annotations == null || annotations.isEmpty()) {
            return;
        }
        builder.startArray("annotations");
        for (Class<?> key : annotations.keySet().stream().sorted().collect(Collectors.toList())) {
            visitAnnotation(annotations.get(key), builder);
        }
        builder.endArray();
    }

    private static void visitAnnotation(Object annotation, XContentBuilderWrapper builder) {
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
    private static void visitAsmMethod(org.objectweb.asm.commons.Method asmMethod, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("name", asmMethod.getName());
        builder.field("descriptor", asmMethod.getDescriptor());
        builder.field("returnType", asmMethod.getReturnType().getClassName());
        builder.field("argumentTypes", Arrays.stream(asmMethod.getArgumentTypes()).map(Type::getClassName));
        builder.endObject();
    }

    // java.lang.invoke
    private static void visitMethodType(MethodType methodType, XContentBuilderWrapper builder) {
        builder.startObject();
        List<Class<?>> parameters = methodType.parameterList();
        if (parameters.isEmpty() == false) {
            builder.field("parameters", classNames(parameters));
        }
        builder.field("return", methodType.returnType().getSimpleName());
        builder.endObject();
    }

    // java.lang.reflect
    private static void visitField(Field field, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field("name", field.getName());
        builder.field("type", field.getType().getSimpleName());
        builder.field("modifiers", Modifier.toString(field.getModifiers()));
        builder.endObject();
    }

    private static void visitMethod(Method method, XContentBuilderWrapper builder) {
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

    private static void visitConstructor(Constructor<?> constructor, XContentBuilderWrapper builder) {
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
