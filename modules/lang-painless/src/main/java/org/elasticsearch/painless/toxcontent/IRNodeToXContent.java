/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless.toxcontent;

import org.elasticsearch.painless.ir.ArgumentsNode;
import org.elasticsearch.painless.ir.BinaryImplNode;
import org.elasticsearch.painless.ir.BinaryMathNode;
import org.elasticsearch.painless.ir.BinaryNode;
import org.elasticsearch.painless.ir.BlockNode;
import org.elasticsearch.painless.ir.BooleanNode;
import org.elasticsearch.painless.ir.BreakNode;
import org.elasticsearch.painless.ir.CastNode;
import org.elasticsearch.painless.ir.CatchNode;
import org.elasticsearch.painless.ir.ClassNode;
import org.elasticsearch.painless.ir.ComparisonNode;
import org.elasticsearch.painless.ir.ConditionNode;
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
import org.elasticsearch.painless.ir.UnaryNode;
import org.elasticsearch.painless.ir.WhileLoopNode;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class IRNodeToXContent {
    static final class Fields {
        static final String NODE = "node";
        static final String LOCATION = "location";
        static final String LEFT = "left";
        static final String RIGHT = "right";
    }

    public static void visitBinaryImpl(BinaryImplNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitBinaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitBinaryMath(BinaryMathNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitBinaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitBlock(BlockNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getStatementsNodes().isEmpty() == false) {
            builder.startArray("statements");
            for (StatementNode statementNode : irNode.getStatementsNodes()) {
                visitStatement(statementNode, builder);
            }
            builder.endArray();
        }
        end(irNode, builder);
    }

    public static void visitBoolean(BooleanNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitBinaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitBreak(BreakNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitCast(CastNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        builder.field("expression");
        visitIR(irNode.getChildNode(), builder);
        end(irNode, builder);
    }

    public static void visitCatch(CatchNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        builder.field("block");
        visitBlock(irNode.getBlockNode(), builder);
        end(irNode, builder);
    }

    public static void visitClass(ClassNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getFieldsNodes().isEmpty() == false) {
            builder.startArray("fields");
            for (FieldNode field : irNode.getFieldsNodes()) {
                visitField(field, builder);
            }
            builder.endArray();
        }

        if (irNode.getFunctionsNodes().isEmpty() == false) {
            builder.startArray("functions");
            for (FunctionNode function : irNode.getFunctionsNodes()) {
                visitFunction(function, builder);
            }
            builder.endArray();
        }
        builder.field("clinitBlock");
        visitBlock(irNode.getClinitBlockNode(), builder);
        end(irNode, builder);
    }

    public static void visitComparison(ComparisonNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitBinaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitConditional(ConditionalNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);

        if (irNode.getConditionNode() != null) {
            builder.field("condition");
            visitIR(irNode.getConditionNode(), builder);
        }

        visitBinaryFragment(irNode, builder);

        end(irNode, builder);
    }

    public static void visitConstant(ConstantNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitContinue(ContinueNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitDeclarationBlock(DeclarationBlockNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getDeclarationsNodes().isEmpty() == false) {
            builder.startArray("declarations");
            for (DeclarationNode declaration : irNode.getDeclarationsNodes()) {
                visitDeclaration(declaration, builder);
            }
            builder.endArray();
        }
        end(irNode, builder);
    }

    public static void visitDeclaration(DeclarationNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getExpressionNode() == null) {
            builder.field("expression");
            visitIR(irNode.getExpressionNode(), builder);
        }
        end(irNode, builder);
    }

    public static void visitDefInterfaceReference(DefInterfaceReferenceNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitDoWhileLoop(DoWhileLoopNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitConditionFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitDup(DupNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitElvis(ElvisNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitBinaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitField(FieldNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitFlipArrayIndex(FlipArrayIndexNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitFlipCollectionIndex(FlipCollectionIndexNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitFlipDefIndex(FlipDefIndexNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitForEachLoop(ForEachLoopNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getConditionNode() != null) {
            builder.field("condition");
            visitConditionFragment(irNode.getConditionNode(), builder);
        }
        end(irNode, builder);
    }

    public static void visitForEachSubArray(ForEachSubArrayNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitConditionFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitForEachSubIterable(ForEachSubIterableNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitConditionFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitForLoop(ForLoopNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getInitializerNode() != null) {
            builder.startArray("initializer");
            visitIR(irNode.getInitializerNode(), builder);
            builder.endArray();
        }

        if (irNode.getConditionNode() != null) {
            builder.startArray("condition");
            visitIR(irNode.getConditionNode(), builder);
            builder.endArray();
        }

        if (irNode.getAfterthoughtNode() != null) {
            builder.startArray("afterthought");
            visitIR(irNode.getAfterthoughtNode(), builder);
            builder.endArray();
        }

        if (irNode.getBlockNode() != null) {
            builder.field("block");
            visitBlock(irNode.getBlockNode(), builder);
        }

        end(irNode, builder);
    }

    public static void visitFunction(FunctionNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        builder.field("block");
        visitBlock(irNode.getBlockNode(), builder);
        end(irNode, builder);
    }

    public static void visitIfElse(IfElseNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitConditionFragment(irNode, builder);

        if (irNode.getElseBlockNode() != null) {
            builder.field("else");
            visitBlock(irNode.getElseBlockNode(), builder);
        }
        end(irNode, builder);
    }

    public static void visitIf(IfNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitConditionFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitInstanceof(InstanceofNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitInvokeCallDef(InvokeCallDefNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitArgumentsFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitInvokeCallMember(InvokeCallMemberNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitArgumentsFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitInvokeCall(InvokeCallNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getMethod() != null) {
            builder.field("method");
            UserDecorationToXContent.visitPainlessMethod(irNode.getMethod(), builder);
        }
        if (irNode.getBox() != null) {
            builder.field("box", irNode.getBox().getSimpleName());
        }
        visitArgumentsFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitListInitialization(ListInitializationNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitArgumentsFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitLoadBraceDef(LoadBraceDefNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitLoadBrace(LoadBraceNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitLoadDotArrayLength(LoadDotArrayLengthNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitLoadDotDef(LoadDotDefNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitLoadDot(LoadDotNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitLoadDotShortcut(LoadDotShortcutNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitLoadFieldMember(LoadFieldMemberNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitLoadListShortcut(LoadListShortcutNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitLoadMapShortcut(LoadMapShortcutNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitLoadVariable(LoadVariableNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitMapInitialization(MapInitializationNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getKeyNodes().isEmpty() == false) {
            builder.startArray("keys");
            for (ExpressionNode expression : irNode.getKeyNodes()) {
                visitIR(expression, builder);
            }
            builder.endArray();
        }

        if (irNode.getValueNodes().isEmpty() == false) {
            builder.startArray("values");
            for (ExpressionNode expression : irNode.getValueNodes()) {
                visitIR(expression, builder);
            }
            builder.endArray();
        }
        end(irNode, builder);
    }

    public static void visitNewArray(NewArrayNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitArgumentsFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitNewObject(NewObjectNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitArgumentsFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitNull(NullNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitNullSafeSub(NullSafeSubNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitReturn(ReturnNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getExpressionNode() != null) {
            builder.field("expression");
            visitIR(irNode.getExpressionNode(), builder);
        }
        end(irNode, builder);
    }

    public static void visitStatementExpression(StatementExpressionNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getExpressionNode() != null) {
            builder.field("expression");
            visitIR(irNode.getExpressionNode(), builder);
        }
        end(irNode, builder);
    }

    public static void visitStatic(StaticNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitStoreBraceDef(StoreBraceDefNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitStoreBrace(StoreBraceNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitStoreDotDef(StoreDotDefNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitStoreDot(StoreDotNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitStoreDotShortcut(StoreDotShortcutNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitStoreFieldMember(StoreFieldMemberNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitStoreListShortcut(StoreListShortcutNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitStoreMapShortcut(StoreMapShortcutNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitStoreVariable(StoreVariableNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitStringConcatenation(StringConcatenationNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitArgumentsFragment(irNode, builder);
        end(irNode, builder);
    }

    public static void visitThrow(ThrowNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getExpressionNode() != null) {
            builder.field("expression");
            visitIR(irNode.getExpressionNode(), builder);
        }
        end(irNode, builder);
    }

    public static void visitTry(TryNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getBlockNode() != null) {
            builder.field("block");
            visitBlock(irNode.getBlockNode(), builder);
        }
        if (irNode.getCatchNodes().isEmpty() == false) {
            builder.startArray("catches");
            for (CatchNode catchNode : irNode.getCatchNodes()) {
                visitCatch(catchNode, builder);
            }
            builder.endArray();
        }
        end(irNode, builder);
    }

    public static void visitTypedCaptureReference(TypedCaptureReferenceNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitTypedInterfaceReference(TypedInterfaceReferenceNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitUnaryMath(UnaryMathNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitUnaryFragment(irNode, builder);
        end(irNode, builder);
    }

   public static void visitWhileLoop(WhileLoopNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        visitConditionFragment(irNode, builder);
        end(irNode, builder);
    }


    // abstracts
    private static void visitArgumentsFragment(ArgumentsNode irNode, XContentBuilderWrapper builder) {
        if (irNode.getArgumentNodes().isEmpty() == false) {
            builder.startArray("arguments");
            for (ExpressionNode expression : irNode.getArgumentNodes()) {
                visitIR((IRNode) expression, builder);
            }
            builder.endArray();
        }
    }

    private static void visitBinaryFragment(BinaryNode irNode, XContentBuilderWrapper builder) {
        builder.startArray(Fields.LEFT);
        visitIR(irNode.getLeftNode(), builder);
        builder.endArray();

        builder.startArray(Fields.RIGHT);
        visitIR(irNode.getRightNode(), builder);
        builder.endArray();
    }

    private static void visitConditionFragment(ConditionNode irNode, XContentBuilderWrapper builder) {
        if (irNode.getConditionNode() != null) {
            builder.field("condition");
            visitIR(irNode.getConditionNode(), builder);
        }

        if (irNode.getBlockNode() != null) {
            builder.field("block");
            visitBlock(irNode.getBlockNode(), builder);
        }
    }

    private static void visitUnaryFragment(UnaryNode irNode, XContentBuilderWrapper builder) {
        if (irNode.getChildNode() != null) {
            builder.field("child");
            visitIR(irNode.getChildNode(), builder);
        }
    }

    private static void visitExpressionFragment(ExpressionNode irNode, XContentBuilderWrapper builder) {
        // TODO(stu): this should never be used, all expression nodes should be handled by their child class version
        throw new IllegalStateException("should be implemented in child [" + irNode.getClass().getSimpleName() + "]");
    }

    public static void visitStatement(StatementNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void visitIR(IRNode irNode, XContentBuilderWrapper builder) {
        if (irNode instanceof BinaryMathNode) {
            visitBinaryMath((BinaryMathNode) irNode, builder);
        } else if (irNode instanceof BinaryImplNode) {
            visitBinaryImpl((BinaryImplNode) irNode, builder);
        } else if (irNode instanceof BlockNode) {
            visitBlock((BlockNode) irNode, builder);
        } else if (irNode instanceof BooleanNode) {
            visitBoolean((BooleanNode) irNode, builder);
        } else if (irNode instanceof BreakNode) {
            visitBreak((BreakNode) irNode, builder);
        } else if (irNode instanceof CastNode) {
            visitCast((CastNode) irNode, builder);
        } else if (irNode instanceof CatchNode) {
            visitCatch((CatchNode) irNode, builder);
        } else if (irNode instanceof ClassNode) {
            visitClass((ClassNode) irNode, builder);
        } else if (irNode instanceof ComparisonNode) {
            visitComparison((ComparisonNode) irNode, builder);
        } else if (irNode instanceof ConditionalNode) {
            visitConditional((ConditionalNode) irNode, builder);
        } else if (irNode instanceof ConstantNode) {
            visitConstant((ConstantNode) irNode, builder);
        } else if (irNode instanceof ContinueNode) {
            visitContinue((ContinueNode) irNode, builder);
        } else if (irNode instanceof DeclarationBlockNode) {
            visitDeclarationBlock((DeclarationBlockNode) irNode, builder);
        } else if (irNode instanceof DeclarationNode) {
            visitDeclaration((DeclarationNode) irNode, builder);
        } else if (irNode instanceof DefInterfaceReferenceNode) {
            visitDefInterfaceReference((DefInterfaceReferenceNode) irNode, builder);
        } else if (irNode instanceof DoWhileLoopNode) {
            visitDoWhileLoop((DoWhileLoopNode) irNode, builder);
        } else if (irNode instanceof DupNode) {
            visitDup((DupNode) irNode, builder);
        } else if (irNode instanceof ElvisNode) {
            visitElvis((ElvisNode) irNode, builder);
        } else if (irNode instanceof FieldNode) {
            visitField((FieldNode) irNode, builder);
        } else if (irNode instanceof FlipArrayIndexNode) {
            visitFlipArrayIndex((FlipArrayIndexNode) irNode, builder);
        } else if (irNode instanceof FlipCollectionIndexNode) {
            visitFlipCollectionIndex((FlipCollectionIndexNode) irNode, builder);
        } else if (irNode instanceof FlipDefIndexNode) {
            visitFlipDefIndex((FlipDefIndexNode) irNode, builder);
        } else if (irNode instanceof ForEachLoopNode) {
            visitForEachLoop((ForEachLoopNode) irNode, builder);
        } else if (irNode instanceof ForEachSubArrayNode) {
            visitForEachSubArray((ForEachSubArrayNode) irNode, builder);
        } else if (irNode instanceof ForEachSubIterableNode) {
            visitForEachSubIterable((ForEachSubIterableNode) irNode, builder);
        } else if (irNode instanceof ForLoopNode) {
            visitForLoop((ForLoopNode) irNode, builder);
        } else if (irNode instanceof FunctionNode) {
            visitFunction((FunctionNode) irNode, builder);
        } else if (irNode instanceof IfElseNode) {
            visitIfElse((IfElseNode) irNode, builder);
        } else if (irNode instanceof IfNode) {
            visitIf((IfNode) irNode, builder);
        } else if (irNode instanceof InstanceofNode) {
            visitInstanceof((InstanceofNode) irNode, builder);
        } else if (irNode instanceof InvokeCallDefNode) {
            visitInvokeCallDef((InvokeCallDefNode) irNode, builder);
        } else if (irNode instanceof InvokeCallMemberNode) {
            visitInvokeCallMember((InvokeCallMemberNode) irNode, builder);
        } else if (irNode instanceof InvokeCallNode) {
            visitInvokeCall((InvokeCallNode) irNode, builder);
        } else if (irNode instanceof ListInitializationNode) {
            visitListInitialization((ListInitializationNode) irNode, builder);
        } else if (irNode instanceof LoadBraceDefNode) {
            visitLoadBraceDef((LoadBraceDefNode) irNode, builder);
        } else if (irNode instanceof LoadBraceNode) {
            visitLoadBrace((LoadBraceNode) irNode, builder);
        } else if (irNode instanceof LoadDotArrayLengthNode) {
            visitLoadDotArrayLength((LoadDotArrayLengthNode) irNode, builder);
        } else if (irNode instanceof LoadDotDefNode) {
            visitLoadDotDef((LoadDotDefNode) irNode, builder);
        } else if (irNode instanceof LoadDotNode) {
            visitLoadDot((LoadDotNode) irNode, builder);
        } else if (irNode instanceof LoadDotShortcutNode) {
            visitLoadDotShortcut((LoadDotShortcutNode) irNode, builder);
        } else if (irNode instanceof LoadFieldMemberNode) {
            visitLoadFieldMember((LoadFieldMemberNode) irNode, builder);
        } else if (irNode instanceof LoadListShortcutNode) {
            visitLoadListShortcut((LoadListShortcutNode) irNode, builder);
        } else if (irNode instanceof LoadMapShortcutNode) {
            visitLoadMapShortcut((LoadMapShortcutNode) irNode, builder);
        } else if (irNode instanceof LoadVariableNode) {
            visitLoadVariable((LoadVariableNode) irNode, builder);
        } else if (irNode instanceof MapInitializationNode) {
            visitMapInitialization((MapInitializationNode) irNode, builder);
        } else if (irNode instanceof NewArrayNode) {
            visitNewArray((NewArrayNode) irNode, builder);
        } else if (irNode instanceof NewObjectNode) {
            visitNewObject((NewObjectNode) irNode, builder);
        } else if (irNode instanceof NullNode) {
            visitNull((NullNode) irNode, builder);
        } else if (irNode instanceof NullSafeSubNode) {
            visitNullSafeSub((NullSafeSubNode) irNode, builder);
        } else if (irNode instanceof ReturnNode) {
            visitReturn((ReturnNode) irNode, builder);
        } else if (irNode instanceof StatementExpressionNode) {
            visitStatementExpression((StatementExpressionNode) irNode, builder);
        } else if (irNode instanceof StaticNode) {
            visitStatic((StaticNode) irNode, builder);
        } else if (irNode instanceof StoreBraceDefNode) {
            visitStoreBraceDef((StoreBraceDefNode) irNode, builder);
        } else if (irNode instanceof StoreBraceNode) {
            visitStoreBrace((StoreBraceNode) irNode, builder);
        } else if (irNode instanceof StoreDotDefNode) {
            visitStoreDotDef((StoreDotDefNode) irNode, builder);
        } else if (irNode instanceof StoreDotNode) {
            visitStoreDot((StoreDotNode) irNode, builder);
        } else if (irNode instanceof StoreDotShortcutNode) {
            visitStoreDotShortcut((StoreDotShortcutNode) irNode, builder);
        } else if (irNode instanceof StoreFieldMemberNode) {
            visitStoreFieldMember((StoreFieldMemberNode) irNode, builder);
        } else if (irNode instanceof StoreListShortcutNode) {
            visitStoreListShortcut((StoreListShortcutNode) irNode, builder);
        } else if (irNode instanceof StoreMapShortcutNode) {
            visitStoreMapShortcut((StoreMapShortcutNode) irNode, builder);
        } else if (irNode instanceof StoreVariableNode) {
            visitStoreVariable((StoreVariableNode) irNode, builder);
        } else if (irNode instanceof StringConcatenationNode) {
            visitStringConcatenation((StringConcatenationNode) irNode, builder);
        } else if (irNode instanceof ThrowNode) {
            visitThrow((ThrowNode) irNode, builder);
        } else if (irNode instanceof TryNode) {
            visitTry((TryNode) irNode, builder);
        } else if (irNode instanceof TypedCaptureReferenceNode) {
            visitTypedCaptureReference((TypedCaptureReferenceNode) irNode, builder);
        } else if (irNode instanceof TypedInterfaceReferenceNode) {
            visitTypedInterfaceReference((TypedInterfaceReferenceNode) irNode, builder);
        } else if (irNode instanceof UnaryMathNode) {
            visitUnaryMath((UnaryMathNode) irNode, builder);
        } else if (irNode instanceof WhileLoopNode) {
            visitWhileLoop((WhileLoopNode) irNode, builder);
        } else if (irNode instanceof ArgumentsNode) { // abstracts
            visitArgumentsFragment((ArgumentsNode) irNode, builder);
        } else if (irNode instanceof BinaryNode) {
            start(irNode, builder);
            visitBinaryFragment((BinaryNode) irNode, builder);
            end(irNode, builder);
        } else if (irNode instanceof ConditionNode) {
            visitConditionFragment((ConditionNode) irNode, builder);
        } else if (irNode instanceof UnaryNode) {
            visitUnaryFragment((UnaryNode) irNode, builder);
        } else if (irNode instanceof ExpressionNode) {
            visitExpressionFragment((ExpressionNode) irNode, builder);
        } else if (irNode instanceof StatementNode) {
            visitStatement((StatementNode) irNode, builder);
        } else {
            start(irNode, builder);
            end(irNode, builder);
        }
    }

    private static void terminal(IRNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        end(irNode, builder);
    }

    private static void start(IRNode irNode, XContentBuilderWrapper builder) {
        builder.startObject();
        builder.field(Fields.NODE, irNode.getClass().getSimpleName());
        builder.field(Fields.LOCATION, irNode.getLocation().getOffset());
    }

    private static void end(IRNode irNode, XContentBuilderWrapper builder) {
        List<Class<? extends IRNode.IRCondition>> conditions = irNode.getAllConditions();
        if (conditions.isEmpty() == false) {
            builder.field(
                UserTreeToXContent.Fields.CONDITIONS,
                conditions.stream().map(Class::getSimpleName).sorted().collect(Collectors.toList())
            );
        }

        List<IRNode.IRDecoration<?>> decorations = irNode.getAllDecorations().stream().sorted(
                    Comparator.comparing(o -> o.getClass().getSimpleName())
                ).collect(Collectors.toList());

        if (decorations.isEmpty() == false) {
            builder.startArray(UserTreeToXContent.Fields.DECORATIONS);
            for (IRNode.IRDecoration<?> decoration : decorations) {
                IRDecorationToXContent.visitIRDecoration(decoration, builder);
            }
            builder.endArray();
        }
        builder.endObject();
    }
}
