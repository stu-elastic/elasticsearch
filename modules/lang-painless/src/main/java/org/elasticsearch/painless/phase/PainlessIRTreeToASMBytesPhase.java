/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless.phase;


import org.elasticsearch.painless.ClassWriter;
import org.elasticsearch.painless.DefBootstrap;
import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.MethodWriter;
import org.elasticsearch.painless.Operation;
import org.elasticsearch.painless.ScriptClassInfo;
import org.elasticsearch.painless.WriterConstants;
import org.elasticsearch.painless.api.Augmentation;
import org.elasticsearch.painless.ir.BinaryImplNode;
import org.elasticsearch.painless.ir.BinaryMathNode;
import org.elasticsearch.painless.ir.BlockNode;
import org.elasticsearch.painless.ir.BooleanNode;
import org.elasticsearch.painless.ir.BreakNode;
import org.elasticsearch.painless.ir.CastNode;
import org.elasticsearch.painless.ir.CatchNode;
import org.elasticsearch.painless.ir.ClassNode;
import org.elasticsearch.painless.ir.ComparisonNode;
import org.elasticsearch.painless.ir.ConditionalNode;
import org.elasticsearch.painless.ir.ConstantNode;
import org.elasticsearch.painless.ir.ContinueNode;
import org.elasticsearch.painless.ir.DeclarationBlockNode;
import org.elasticsearch.painless.ir.DeclarationNode;
import org.elasticsearch.painless.ir.DefInterfaceReferenceNode;
import org.elasticsearch.painless.ir.DoWhileLoopNode;
import org.elasticsearch.painless.ir.DupNode;
import org.elasticsearch.painless.ir.ElvisNode;
import org.elasticsearch.painless.ir.ExpressionNode;
import org.elasticsearch.painless.ir.FieldNode;
import org.elasticsearch.painless.ir.FlipArrayIndexNode;
import org.elasticsearch.painless.ir.FlipCollectionIndexNode;
import org.elasticsearch.painless.ir.FlipDefIndexNode;
import org.elasticsearch.painless.ir.ForEachLoopNode;
import org.elasticsearch.painless.ir.ForEachSubArrayNode;
import org.elasticsearch.painless.ir.ForEachSubIterableNode;
import org.elasticsearch.painless.ir.ForLoopNode;
import org.elasticsearch.painless.ir.FunctionNode;
import org.elasticsearch.painless.ir.IRNode;
import org.elasticsearch.painless.ir.IfElseNode;
import org.elasticsearch.painless.ir.IfNode;
import org.elasticsearch.painless.ir.InstanceofNode;
import org.elasticsearch.painless.ir.InvokeCallDefNode;
import org.elasticsearch.painless.ir.InvokeCallMemberNode;
import org.elasticsearch.painless.ir.InvokeCallNode;
import org.elasticsearch.painless.ir.ListInitializationNode;
import org.elasticsearch.painless.ir.LoadBraceDefNode;
import org.elasticsearch.painless.ir.LoadBraceNode;
import org.elasticsearch.painless.ir.LoadDotArrayLengthNode;
import org.elasticsearch.painless.ir.LoadDotDefNode;
import org.elasticsearch.painless.ir.LoadDotNode;
import org.elasticsearch.painless.ir.LoadDotShortcutNode;
import org.elasticsearch.painless.ir.LoadFieldMemberNode;
import org.elasticsearch.painless.ir.LoadListShortcutNode;
import org.elasticsearch.painless.ir.LoadMapShortcutNode;
import org.elasticsearch.painless.ir.LoadVariableNode;
import org.elasticsearch.painless.ir.MapInitializationNode;
import org.elasticsearch.painless.ir.NewArrayNode;
import org.elasticsearch.painless.ir.NewObjectNode;
import org.elasticsearch.painless.ir.NullNode;
import org.elasticsearch.painless.ir.NullSafeSubNode;
import org.elasticsearch.painless.ir.ReturnNode;
import org.elasticsearch.painless.ir.StatementExpressionNode;
import org.elasticsearch.painless.ir.StatementNode;
import org.elasticsearch.painless.ir.StaticNode;
import org.elasticsearch.painless.ir.StoreBraceDefNode;
import org.elasticsearch.painless.ir.StoreBraceNode;
import org.elasticsearch.painless.ir.StoreDotDefNode;
import org.elasticsearch.painless.ir.StoreDotNode;
import org.elasticsearch.painless.ir.StoreDotShortcutNode;
import org.elasticsearch.painless.ir.StoreFieldMemberNode;
import org.elasticsearch.painless.ir.StoreListShortcutNode;
import org.elasticsearch.painless.ir.StoreMapShortcutNode;
import org.elasticsearch.painless.ir.StoreVariableNode;
import org.elasticsearch.painless.ir.StringConcatenationNode;
import org.elasticsearch.painless.ir.ThrowNode;
import org.elasticsearch.painless.ir.TryNode;
import org.elasticsearch.painless.ir.TypedCaptureReferenceNode;
import org.elasticsearch.painless.ir.TypedInterfaceReferenceNode;
import org.elasticsearch.painless.ir.UnaryMathNode;
import org.elasticsearch.painless.ir.WhileLoopNode;
import org.elasticsearch.painless.lookup.PainlessClassBinding;
import org.elasticsearch.painless.lookup.PainlessConstructor;
import org.elasticsearch.painless.lookup.PainlessField;
import org.elasticsearch.painless.lookup.PainlessInstanceBinding;
import org.elasticsearch.painless.lookup.PainlessLookupUtility;
import org.elasticsearch.painless.lookup.PainlessMethod;
import org.elasticsearch.painless.lookup.def;
import org.elasticsearch.painless.symbol.FunctionTable.LocalFunction;
import org.elasticsearch.painless.symbol.IRDecorations.IRCAllEscape;
import org.elasticsearch.painless.symbol.IRDecorations.IRCContinuous;
import org.elasticsearch.painless.symbol.IRDecorations.IRCInitialize;
import org.elasticsearch.painless.symbol.IRDecorations.IRCRead;
import org.elasticsearch.painless.symbol.IRDecorations.IRCStatic;
import org.elasticsearch.painless.symbol.IRDecorations.IRCSynthetic;
import org.elasticsearch.painless.symbol.IRDecorations.IRCVarArgs;
import org.elasticsearch.painless.symbol.IRDecorations.IRDArrayName;
import org.elasticsearch.painless.symbol.IRDecorations.IRDArrayType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDBinaryType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDCaptureNames;
import org.elasticsearch.painless.symbol.IRDecorations.IRDCast;
import org.elasticsearch.painless.symbol.IRDecorations.IRDClassBinding;
import org.elasticsearch.painless.symbol.IRDecorations.IRDComparisonType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDConstant;
import org.elasticsearch.painless.symbol.IRDecorations.IRDConstantFieldName;
import org.elasticsearch.painless.symbol.IRDecorations.IRDConstructor;
import org.elasticsearch.painless.symbol.IRDecorations.IRDDeclarationType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDDefReferenceEncoding;
import org.elasticsearch.painless.symbol.IRDecorations.IRDDepth;
import org.elasticsearch.painless.symbol.IRDecorations.IRDExceptionType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDExpressionType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDField;
import org.elasticsearch.painless.symbol.IRDecorations.IRDFieldType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDFlags;
import org.elasticsearch.painless.symbol.IRDecorations.IRDFunction;
import org.elasticsearch.painless.symbol.IRDecorations.IRDIndexName;
import org.elasticsearch.painless.symbol.IRDecorations.IRDIndexType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDIndexedType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDInstanceBinding;
import org.elasticsearch.painless.symbol.IRDecorations.IRDInstanceType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDIterableName;
import org.elasticsearch.painless.symbol.IRDecorations.IRDIterableType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDMaxLoopCounter;
import org.elasticsearch.painless.symbol.IRDecorations.IRDMethod;
import org.elasticsearch.painless.symbol.IRDecorations.IRDModifiers;
import org.elasticsearch.painless.symbol.IRDecorations.IRDName;
import org.elasticsearch.painless.symbol.IRDecorations.IRDOperation;
import org.elasticsearch.painless.symbol.IRDecorations.IRDParameterNames;
import org.elasticsearch.painless.symbol.IRDecorations.IRDReference;
import org.elasticsearch.painless.symbol.IRDecorations.IRDRegexLimit;
import org.elasticsearch.painless.symbol.IRDecorations.IRDReturnType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDShiftType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDSize;
import org.elasticsearch.painless.symbol.IRDecorations.IRDStoreType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDSymbol;
import org.elasticsearch.painless.symbol.IRDecorations.IRDTypeParameters;
import org.elasticsearch.painless.symbol.IRDecorations.IRDUnaryType;
import org.elasticsearch.painless.symbol.IRDecorations.IRDValue;
import org.elasticsearch.painless.symbol.IRDecorations.IRDVariableName;
import org.elasticsearch.painless.symbol.IRDecorations.IRDVariableType;
import org.elasticsearch.painless.symbol.ScriptScope;
import org.elasticsearch.painless.symbol.WriteScope;
import org.elasticsearch.painless.symbol.WriteScope.Variable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.util.Printer;

import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

import static org.elasticsearch.painless.WriterConstants.BASE_INTERFACE_TYPE;
import static org.elasticsearch.painless.WriterConstants.CLASS_TYPE;
import static org.elasticsearch.painless.WriterConstants.EQUALS;
import static org.elasticsearch.painless.WriterConstants.ITERATOR_HASNEXT;
import static org.elasticsearch.painless.WriterConstants.ITERATOR_NEXT;
import static org.elasticsearch.painless.WriterConstants.ITERATOR_TYPE;
import static org.elasticsearch.painless.WriterConstants.OBJECTS_TYPE;


import java.util.List;

public class PainlessIRTreeToASMBytesPhase extends DefaultIRTreeToASMBytesPhase {
    @Override
    public void visitFunction(FunctionNode irFunctionNode, WriteScope writeScope) {
        // TODO(stu): all functions are final methods, put # before
        int access = Opcodes.ACC_PUBLIC;

        if (irFunctionNode.hasCondition(IRCStatic.class)) {
            access |= Opcodes.ACC_STATIC;
        }

        if (irFunctionNode.hasCondition(IRCVarArgs.class)) {
            access |= Opcodes.ACC_VARARGS;
        }

        if (irFunctionNode.hasCondition(IRCSynthetic.class)) {
            access |= Opcodes.ACC_SYNTHETIC;
        }

        Type asmReturnType = MethodWriter.getType(irFunctionNode.getDecorationValue(IRDReturnType.class));
        List<Class<?>> typeParameters = irFunctionNode.getDecorationValue(IRDTypeParameters.class);
        Type[] asmParameterTypes = new Type[typeParameters.size()];

        for (int index = 0; index < asmParameterTypes.length; ++index) {
            asmParameterTypes[index] = MethodWriter.getType(typeParameters.get(index));
        }

        Method method = new Method(irFunctionNode.getDecorationValue(IRDName.class), asmReturnType, asmParameterTypes);

        ClassWriter classWriter = writeScope.getClassWriter();
        MethodWriter methodWriter = classWriter.newMethodWriter(access, method);
        writeScope = writeScope.newMethodScope(methodWriter);

        if (irFunctionNode.hasCondition(IRCStatic.class) == false) {
            writeScope.defineInternalVariable(Object.class, "this");
        }

        List<String> parameterNames = irFunctionNode.getDecorationValue(IRDParameterNames.class);

        for (int index = 0; index < typeParameters.size(); ++index) {
            writeScope.defineVariable(typeParameters.get(index), parameterNames.get(index));
        }

        methodWriter.visitCode();

        if (irFunctionNode.getDecorationValue(IRDMaxLoopCounter.class) > 0) {
            // if there is infinite loop protection, we do this once:
            // int #loop = settings.getMaxLoopCounter()

            Variable loop = writeScope.defineInternalVariable(int.class, "loop");

            methodWriter.push(irFunctionNode.getDecorationValue(IRDMaxLoopCounter.class));
            methodWriter.visitVarInsn(Opcodes.ISTORE, loop.getSlot());
        }

        visit(irFunctionNode.getBlockNode(), writeScope.newBlockScope());

        methodWriter.endMethod();
    }
}
