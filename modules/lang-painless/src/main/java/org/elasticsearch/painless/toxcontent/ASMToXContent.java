/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless.toxcontent;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.painless.MethodWriter;
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
import org.elasticsearch.painless.ir.FieldNode;
import org.elasticsearch.painless.ir.FlipArrayIndexNode;
import org.elasticsearch.painless.ir.FlipCollectionIndexNode;
import org.elasticsearch.painless.ir.FlipDefIndexNode;
import org.elasticsearch.painless.ir.ForEachLoopNode;
import org.elasticsearch.painless.ir.ForEachSubArrayNode;
import org.elasticsearch.painless.ir.ForEachSubIterableNode;
import org.elasticsearch.painless.ir.ForLoopNode;
import org.elasticsearch.painless.ir.FunctionNode;
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
import org.elasticsearch.painless.phase.DefaultIRTreeToASMBytesPhase;
import org.elasticsearch.painless.symbol.WriteScope;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ASMToXContent extends DefaultIRTreeToASMBytesPhase {
    private final XContentBuilderWrapper builder;

    public ASMToXContent(XContentBuilder builder) {
        this.builder = new XContentBuilderWrapper(Objects.requireNonNull(builder));
    }

    public ASMToXContent() {
        this.builder = new XContentBuilderWrapper();
    }

    @Override
    public void visitClass(ClassNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.startArray("class");
        super.visitClass(irNode, writeScope);
        IRNodeToXContent.visitIR(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endArray();
        builder.endObject();
    }

    @Override
    public void visitFunction(FunctionNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("function");
        super.visitFunction(irNode, writeScope);
        IRNodeToXContent.visitFunction(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitField(FieldNode irNode, WriteScope writeScope) {
        System.out.println("--->field--");
        builder.startObject();
        builder.field("field");
        super.visitField(irNode, writeScope);
        IRNodeToXContent.visitField(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
        System.out.println("---field-->");
    }

    @Override
    public void visitBlock(BlockNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("block");
        super.visitBlock(irNode, writeScope);
        IRNodeToXContent.visitBlock(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitIf(IfNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("if");
        super.visitIf(irNode, writeScope);
        IRNodeToXContent.visitIf(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitIfElse(IfElseNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("ifElse");
        super.visitIfElse(irNode, writeScope);
        IRNodeToXContent.visitIfElse(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitWhileLoop(WhileLoopNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("whileLoop");
        super.visitWhileLoop(irNode, writeScope);
        IRNodeToXContent.visitWhileLoop(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitDoWhileLoop(DoWhileLoopNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("doWhileLoop");
        super.visitDoWhileLoop(irNode, writeScope);
        IRNodeToXContent.visitDoWhileLoop(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitForLoop(ForLoopNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("forLoop");
        super.visitForLoop(irNode, writeScope);
        IRNodeToXContent.visitForLoop(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitForEachLoop(ForEachLoopNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("forEachLoop");
        super.visitForEachLoop(irNode, writeScope);
        IRNodeToXContent.visitForEachLoop(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitForEachSubArrayLoop(ForEachSubArrayNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("forEachSubArrayLoop");
        super.visitForEachSubArrayLoop(irNode, writeScope);
        IRNodeToXContent.visitForEachSubArrayLoop(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitForEachSubIterableLoop(ForEachSubIterableNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("forEachSubIterableLoop");
        super.visitForEachSubIterableLoop(irNode, writeScope);
        IRNodeToXContent.visitForEachSubIterableLoop(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitDeclarationBlock(DeclarationBlockNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("declarationBlock");
        super.visitDeclarationBlock(irNode, writeScope);
        IRNodeToXContent.visitDeclarationBlock(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitDeclaration(DeclarationNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("declaration");
        super.visitDeclaration(irNode, writeScope);
        IRNodeToXContent.visitDeclaration(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitReturn(ReturnNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("return");
        super.visitReturn(irNode, writeScope);
        IRNodeToXContent.visitReturn(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitStatementExpression(StatementExpressionNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("statementExpression");
        super.visitStatementExpression(irNode, writeScope);
        IRNodeToXContent.visitStatementExpression(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }


    @Override
    public void visitTry(TryNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("try");
        super.visitTry(irNode, writeScope);
        IRNodeToXContent.visitTry(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitCatch(CatchNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("catch");
        super.visitCatch(irNode, writeScope);
        IRNodeToXContent.visitCatch(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitThrow(ThrowNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("throw");
        super.visitThrow(irNode, writeScope);
        IRNodeToXContent.visitThrow(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitContinue(ContinueNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("continue");
        super.visitContinue(irNode, writeScope);
        IRNodeToXContent.visitContinue(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitBreak(BreakNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("break");
        super.visitBreak(irNode, writeScope);
        IRNodeToXContent.visitBreak(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitBinaryImpl(BinaryImplNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("binaryImpl");
        super.visitBinaryImpl(irNode, writeScope);
        IRNodeToXContent.visitBinaryImpl(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitUnaryMath(UnaryMathNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("unaryMath");
        super.visitUnaryMath(irNode, writeScope);
        IRNodeToXContent.visitUnaryMath(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitBinaryMath(BinaryMathNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("binaryMath");
        super.visitBinaryMath(irNode, writeScope);
        IRNodeToXContent.visitBinaryMath(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitStringConcatenation(StringConcatenationNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("stringConcatenation");
        super.visitStringConcatenation(irNode, writeScope);
        IRNodeToXContent.visitStringConcatenation(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitBoolean(BooleanNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("boolean");
        super.visitBoolean(irNode, writeScope);
        IRNodeToXContent.visitBoolean(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitComparison(ComparisonNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("comparison");
        super.visitComparison(irNode, writeScope);
        IRNodeToXContent.visitComparison(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitCast(CastNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("cast");
        super.visitCast(irNode, writeScope);
        builder.startObject("ir");
        IRNodeToXContent.visitCast(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitInstanceof(InstanceofNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("instanceof");
        super.visitInstanceof(irNode, writeScope);
        IRNodeToXContent.visitInstanceof(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitConditional(ConditionalNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("conditional");
        super.visitConditional(irNode, writeScope);
        IRNodeToXContent.visitConditional(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitElvis(ElvisNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("elvis");
        super.visitElvis(irNode, writeScope);
        IRNodeToXContent.visitElvis(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitListInitialization(ListInitializationNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("listInitialization");
        super.visitListInitialization(irNode, writeScope);
        IRNodeToXContent.visitListInitialization(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitMapInitialization(MapInitializationNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("mapInitialization");
        super.visitMapInitialization(irNode, writeScope);
        IRNodeToXContent.visitMapInitialization(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitNewArray(NewArrayNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("newArray");
        super.visitNewArray(irNode, writeScope);
        IRNodeToXContent.visitNewArray(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitNewObject(NewObjectNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("newObject");
        super.visitNewObject(irNode, writeScope);
        IRNodeToXContent.visitNewObject(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitConstant(ConstantNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.startObject("constant");
        builder.field("ir");
        super.visitConstant(irNode, writeScope);
        IRNodeToXContent.visitConstant(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        System.out.println("finishing constant");
        builder.endObject();
    }

    @Override
    public void visitNull(NullNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("null");
        super.visitNull(irNode, writeScope);
        IRNodeToXContent.visitNull(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitDefInterfaceReference(DefInterfaceReferenceNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("defInterfaceReference");
        super.visitDefInterfaceReference(irNode, writeScope);
        IRNodeToXContent.visitDefInterfaceReference(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitTypedInterfaceReference(TypedInterfaceReferenceNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("typedInterfaceReference");
        super.visitTypedInterfaceReference(irNode, writeScope);
        IRNodeToXContent.visitTypedInterfaceReference(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitTypedCaptureReference(TypedCaptureReferenceNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("typedCaptureReference");
        super.visitTypedCaptureReference(irNode, writeScope);
        IRNodeToXContent.visitTypedCaptureReference(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitStatic(StaticNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("static");
        super.visitStatic(irNode, writeScope);
        IRNodeToXContent.visitStatic(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitLoadVariable(LoadVariableNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("loadVariable");
        super.visitLoadVariable(irNode, writeScope);
        IRNodeToXContent.visitLoadVariable(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitNullSafeSub(NullSafeSubNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("nullSafeSub");
        super.visitNullSafeSub(irNode, writeScope);
        IRNodeToXContent.visitNullSafeSub(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitLoadDotArrayLengthNode(LoadDotArrayLengthNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("loadDotArrayLengthNode");
        super.visitLoadDotArrayLengthNode(irNode, writeScope);
        IRNodeToXContent.visitLoadDotArrayLengthNode(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitLoadDotDef(LoadDotDefNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("loadDotDef");
        super.visitLoadDotDef(irNode, writeScope);
        IRNodeToXContent.visitLoadDotDef(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitLoadDot(LoadDotNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("loadDot");
        super.visitLoadDot(irNode, writeScope);
        IRNodeToXContent.visitLoadDot(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitLoadDotShortcut(LoadDotShortcutNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("loadDotShortcut");
        super.visitLoadDotShortcut(irNode, writeScope);
        IRNodeToXContent.visitLoadDotShortcut(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitLoadListShortcut(LoadListShortcutNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("loadListShortcut");
        super.visitLoadListShortcut(irNode, writeScope);
        IRNodeToXContent.visitLoadListShortcut(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitLoadMapShortcut(LoadMapShortcutNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("loadMapShortcut");
        super.visitLoadMapShortcut(irNode, writeScope);
        IRNodeToXContent.visitLoadMapShortcut(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitLoadFieldMember(LoadFieldMemberNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("loadFieldMember");
        super.visitLoadFieldMember(irNode, writeScope);
        IRNodeToXContent.visitLoadFieldMember(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitLoadBraceDef(LoadBraceDefNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("loadBraceDef");
        super.visitLoadBraceDef(irNode, writeScope);
        IRNodeToXContent.visitLoadBraceDef(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitLoadBrace(LoadBraceNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("loadBrace");
        super.visitLoadBrace(irNode, writeScope);
        IRNodeToXContent.visitLoadBrace(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitStoreVariable(StoreVariableNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("storeVariable");
        super.visitStoreVariable(irNode, writeScope);
        IRNodeToXContent.visitStoreVariable(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitStoreDotDef(StoreDotDefNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("storeDotDef");
        super.visitStoreDotDef(irNode, writeScope);
        IRNodeToXContent.visitStoreDotDef(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitStoreDot(StoreDotNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("storeDot");
        super.visitStoreDot(irNode, writeScope);
        IRNodeToXContent.visitStoreDot(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitStoreDotShortcut(StoreDotShortcutNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("storeDotShortcut");
        super.visitStoreDotShortcut(irNode, writeScope);
        IRNodeToXContent.visitStoreDotShortcut(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitStoreListShortcut(StoreListShortcutNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("storeListShortcut");
        super.visitStoreListShortcut(irNode, writeScope);
        IRNodeToXContent.visitStoreListShortcut(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitStoreMapShortcut(StoreMapShortcutNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("storeMapShortcut");
        super.visitStoreMapShortcut(irNode, writeScope);
        IRNodeToXContent.visitStoreMapShortcut(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitStoreFieldMember(StoreFieldMemberNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("storeFieldMember");
        super.visitStoreFieldMember(irNode, writeScope);
        IRNodeToXContent.visitStoreFieldMember(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitStoreBraceDef(StoreBraceDefNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("storeBraceDef");
        super.visitStoreBraceDef(irNode, writeScope);
        IRNodeToXContent.visitStoreBraceDef(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitStoreBrace(StoreBraceNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("storeBrace");
        super.visitStoreBrace(irNode, writeScope);
        IRNodeToXContent.visitStoreBrace(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitInvokeCallDef(InvokeCallDefNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("invokeCallDef");
        super.visitInvokeCallDef(irNode, writeScope);
        IRNodeToXContent.visitInvokeCallDef(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitInvokeCall(InvokeCallNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("invokeCall");
        super.visitInvokeCall(irNode, writeScope);
        IRNodeToXContent.visitInvokeCall(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitInvokeCallMember(InvokeCallMemberNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("invokeCallMember");
        super.visitInvokeCallMember(irNode, writeScope);
        IRNodeToXContent.visitInvokeCallMember(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitFlipArrayIndex(FlipArrayIndexNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("flipArrayIndex");
        super.visitFlipArrayIndex(irNode, writeScope);
        IRNodeToXContent.visitFlipArrayIndex(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitFlipCollectionIndex(FlipCollectionIndexNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("flipCollectionIndex");
        super.visitFlipCollectionIndex(irNode, writeScope);
        IRNodeToXContent.visitFlipCollectionIndex(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitFlipDefIndex(FlipDefIndexNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("flipDefIndex");
        super.visitFlipDefIndex(irNode, writeScope);
        IRNodeToXContent.visitFlipDefIndex(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    @Override
    public void visitDup(DupNode irNode, WriteScope writeScope) {
        builder.startObject();
        builder.field("dup");
        super.visitDup(irNode, writeScope);
        IRNodeToXContent.visitDup(irNode, builder);
        if (writeScope.isEmpty() == false) {
            visitWriteScopeFragment(writeScope);
        }
        builder.endObject();
    }

    private void visitWriteScopeFragment(WriteScope writeScope) {
        System.out.println("startObject-writeScope");
        builder.startObject("writeScope");

        WriteScope parent = writeScope.getParent();
        if (parent != null) {
            /*if (parent.isEmpty()) {
                builder.field("parent", "empty");
            } else {

            }*/
            System.out.println("startObject-writeScope-parent");
            builder.startObject("parent");
            visitWriteScopeFragment(parent);
            System.out.println("endObject-writeScope-parent");
            builder.endObject();
        }

        MethodWriter writer = writeScope.getMethodWriter();
        if (writer != null) {
            builder.field("methodWriter");
            visitMethodWriter(writer);
        }

        Label label = writeScope.getContinueLabel();
        if (label != null) {
            builder.field("continueLabel", label.getOffset());
        }

        label = writeScope.getBreakLabel();
        if (label != null) {
            builder.field("breakLabel", label.getOffset());
        }

        label = writeScope.getTryBeginLabel();
        if (label != null) {
            builder.field("tryBeginLabel", label.getOffset());
        }

        label = writeScope.getTryEndLabel();
        if (label != null) {
            builder.field("tryEndLabel", label.getOffset());
        }

        label = writeScope.getCatchesEndLabel();
        if (label != null) {
            builder.field("catchesEndLabel", label.getOffset());
        }

        if (writeScope.getNextSlot() > 0) {
            builder.field("nextSlot", writeScope.getNextSlot());
        }

        Map<String, WriteScope.Variable> variables = writeScope.getVariables();
        if (variables != null && variables.isEmpty() == false) {
            builder.startArray("variables");
            for (String name : variables.keySet().stream().sorted().collect(Collectors.toList())) {
                WriteScope.Variable var = variables.get(name);
                if (var != null) {
                    visitVariable(variables.get(name));
                }
            }
            builder.endArray();
        }

        builder.endObject();
    }

    private void visitMethodWriter(MethodWriter writer) {
        builder.startObject();
        builder.field("access", writer.getAccess());
        String name = writer.getName();
        if (name != null) {
            builder.field("name", name);
        }
        Type[] argumentTypes = writer.getArgumentTypes();
        if (argumentTypes != null && argumentTypes.length > 0) {
            builder.startArray("argumentTypes");
            for (Type argumentType : argumentTypes) {
                builder.value(argumentType.getClassName());
            }
            builder.endArray();
        }
        Type returnType = writer.getReturnType();
        if (returnType != null) {
            builder.field("returnType", returnType.getClassName());
        }
        builder.endObject();
    }

    private void visitVariable(WriteScope.Variable variable) {
        builder.startObject();
        if (variable.getName() != null) {
            builder.field("name", variable.getName());
        }
        if (variable.getType() != null) {
            builder.field("type", variable.getType().getSimpleName());
        }
        if (variable.getAsmType() != null) {
            builder.field("asmType", variable.getAsmType().getClassName());
        }
        builder.field("slot", variable.getSlot());
        builder.endObject();
    }
}
