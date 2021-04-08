/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless.toxcontent;

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
import org.elasticsearch.painless.phase.IRTreeVisitor;
import org.elasticsearch.painless.symbol.WriteScope;

public class IRTreeToXContent implements IRTreeVisitor<WriteScope> {
    @Override
    public void visitClass(ClassNode irClassNode, WriteScope writeScope) {

    }

    @Override
    public void visitFunction(FunctionNode irFunctionNode, WriteScope writeScope) {

    }

    @Override
    public void visitField(FieldNode irFieldNode, WriteScope writeScope) {

    }

    @Override
    public void visitBlock(BlockNode irBlockNode, WriteScope writeScope) {

    }

    @Override
    public void visitIf(IfNode irIfNode, WriteScope writeScope) {

    }

    @Override
    public void visitIfElse(IfElseNode irIfElseNode, WriteScope writeScope) {

    }

    @Override
    public void visitWhileLoop(WhileLoopNode irWhileLoopNode, WriteScope writeScope) {

    }

    @Override
    public void visitDoWhileLoop(DoWhileLoopNode irDoWhileLoopNode, WriteScope writeScope) {

    }

    @Override
    public void visitForLoop(ForLoopNode irForLoopNode, WriteScope writeScope) {

    }

    @Override
    public void visitForEachLoop(ForEachLoopNode irForEachLoopNode, WriteScope writeScope) {

    }

    @Override
    public void visitForEachSubArrayLoop(ForEachSubArrayNode irForEachSubArrayNode, WriteScope writeScope) {

    }

    @Override
    public void visitForEachSubIterableLoop(ForEachSubIterableNode irForEachSubIterableNode, WriteScope writeScope) {

    }

    @Override
    public void visitDeclarationBlock(DeclarationBlockNode irDeclarationBlockNode, WriteScope writeScope) {

    }

    @Override
    public void visitDeclaration(DeclarationNode irDeclarationNode, WriteScope writeScope) {

    }

    @Override
    public void visitReturn(ReturnNode irReturnNode, WriteScope writeScope) {

    }

    @Override
    public void visitStatementExpression(StatementExpressionNode irStatementExpressionNode, WriteScope writeScope) {

    }

    @Override
    public void visitTry(TryNode irTryNode, WriteScope writeScope) {

    }

    @Override
    public void visitCatch(CatchNode irCatchNode, WriteScope writeScope) {

    }

    @Override
    public void visitThrow(ThrowNode irThrowNode, WriteScope writeScope) {

    }

    @Override
    public void visitContinue(ContinueNode irContinueNode, WriteScope writeScope) {

    }

    @Override
    public void visitBreak(BreakNode irBreakNode, WriteScope writeScope) {

    }

    @Override
    public void visitBinaryImpl(BinaryImplNode irBinaryImplNode, WriteScope writeScope) {

    }

    @Override
    public void visitUnaryMath(UnaryMathNode irUnaryMathNode, WriteScope writeScope) {

    }

    @Override
    public void visitBinaryMath(BinaryMathNode irBinaryMathNode, WriteScope writeScope) {

    }

    @Override
    public void visitStringConcatenation(StringConcatenationNode irStringConcatenationNode, WriteScope writeScope) {

    }

    @Override
    public void visitBoolean(BooleanNode irBooleanNode, WriteScope writeScope) {

    }

    @Override
    public void visitComparison(ComparisonNode irComparisonNode, WriteScope writeScope) {

    }

    @Override
    public void visitCast(CastNode irCastNode, WriteScope writeScope) {

    }

    @Override
    public void visitInstanceof(InstanceofNode irInstanceofNode, WriteScope writeScope) {

    }

    @Override
    public void visitConditional(ConditionalNode irConditionalNode, WriteScope writeScope) {

    }

    @Override
    public void visitElvis(ElvisNode irElvisNode, WriteScope writeScope) {

    }

    @Override
    public void visitListInitialization(ListInitializationNode irListInitializationNode, WriteScope writeScope) {

    }

    @Override
    public void visitMapInitialization(MapInitializationNode irMapInitializationNode, WriteScope writeScope) {

    }

    @Override
    public void visitNewArray(NewArrayNode irNewArrayNode, WriteScope writeScope) {

    }

    @Override
    public void visitNewObject(NewObjectNode irNewObjectNode, WriteScope writeScope) {

    }

    @Override
    public void visitConstant(ConstantNode irConstantNode, WriteScope writeScope) {

    }

    @Override
    public void visitNull(NullNode irNullNode, WriteScope writeScope) {

    }

    @Override
    public void visitDefInterfaceReference(DefInterfaceReferenceNode irDefInterfaceReferenceNode, WriteScope writeScope) {

    }

    @Override
    public void visitTypedInterfaceReference(TypedInterfaceReferenceNode irTypedInterfaceReferenceNode, WriteScope writeScope) {

    }

    @Override
    public void visitTypedCaptureReference(TypedCaptureReferenceNode irTypedCaptureReferenceNode, WriteScope writeScope) {

    }

    @Override
    public void visitStatic(StaticNode irStaticNode, WriteScope writeScope) {

    }

    @Override
    public void visitLoadVariable(LoadVariableNode irLoadVariableNode, WriteScope writeScope) {

    }

    @Override
    public void visitNullSafeSub(NullSafeSubNode irNullSafeSubNode, WriteScope writeScope) {

    }

    @Override
    public void visitLoadDotArrayLengthNode(LoadDotArrayLengthNode irLoadDotArrayLengthNode, WriteScope writeScope) {

    }

    @Override
    public void visitLoadDotDef(LoadDotDefNode irLoadDotDefNode, WriteScope writeScope) {

    }

    @Override
    public void visitLoadDot(LoadDotNode irLoadDotNode, WriteScope writeScope) {

    }

    @Override
    public void visitLoadDotShortcut(LoadDotShortcutNode irDotSubShortcutNode, WriteScope writeScope) {

    }

    @Override
    public void visitLoadListShortcut(LoadListShortcutNode irLoadListShortcutNode, WriteScope writeScope) {

    }

    @Override
    public void visitLoadMapShortcut(LoadMapShortcutNode irLoadMapShortcutNode, WriteScope writeScope) {

    }

    @Override
    public void visitLoadFieldMember(LoadFieldMemberNode irLoadFieldMemberNode, WriteScope writeScope) {

    }

    @Override
    public void visitLoadBraceDef(LoadBraceDefNode irLoadBraceDefNode, WriteScope writeScope) {

    }

    @Override
    public void visitLoadBrace(LoadBraceNode irLoadBraceNode, WriteScope writeScope) {

    }

    @Override
    public void visitStoreVariable(StoreVariableNode irStoreVariableNode, WriteScope writeScope) {

    }

    @Override
    public void visitStoreDotDef(StoreDotDefNode irStoreDotDefNode, WriteScope writeScope) {

    }

    @Override
    public void visitStoreDot(StoreDotNode irStoreDotNode, WriteScope writeScope) {

    }

    @Override
    public void visitStoreDotShortcut(StoreDotShortcutNode irDotSubShortcutNode, WriteScope writeScope) {

    }

    @Override
    public void visitStoreListShortcut(StoreListShortcutNode irStoreListShortcutNode, WriteScope writeScope) {

    }

    @Override
    public void visitStoreMapShortcut(StoreMapShortcutNode irStoreMapShortcutNode, WriteScope writeScope) {

    }

    @Override
    public void visitStoreFieldMember(StoreFieldMemberNode irStoreFieldMemberNode, WriteScope writeScope) {

    }

    @Override
    public void visitStoreBraceDef(StoreBraceDefNode irStoreBraceDefNode, WriteScope writeScope) {

    }

    @Override
    public void visitStoreBrace(StoreBraceNode irStoreBraceNode, WriteScope writeScope) {

    }

    @Override
    public void visitInvokeCallDef(InvokeCallDefNode irInvokeCallDefNode, WriteScope writeScope) {

    }

    @Override
    public void visitInvokeCall(InvokeCallNode irInvokeCallNode, WriteScope writeScope) {

    }

    @Override
    public void visitInvokeCallMember(InvokeCallMemberNode irInvokeCallMemberNode, WriteScope writeScope) {

    }

    @Override
    public void visitFlipArrayIndex(FlipArrayIndexNode irFlipArrayIndexNode, WriteScope writeScope) {

    }

    @Override
    public void visitFlipCollectionIndex(FlipCollectionIndexNode irFlipCollectionIndexNode, WriteScope writeScope) {

    }

    @Override
    public void visitFlipDefIndex(FlipDefIndexNode irFlipDefIndexNode, WriteScope writeScope) {

    }

    @Override
    public void visitDup(DupNode irDupNode, WriteScope writeScope) {

    }
}
