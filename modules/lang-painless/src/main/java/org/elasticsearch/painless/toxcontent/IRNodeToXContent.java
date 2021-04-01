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

    public static void toXContent(BinaryImplNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((BinaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(BinaryMathNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((BinaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(BlockNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getStatementsNodes().isEmpty() == false) {
            builder.startArray("statements");
            for (StatementNode statementNode : irNode.getStatementsNodes()) {
                toXContent(statementNode, builder);
            }
            builder.endArray();
        }
        end(irNode, builder);
    }

    public static void toXContent(BooleanNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((BinaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(BreakNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(CastNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        builder.field("expression");
        toXContent(irNode.getChildNode(), builder);
        end(irNode, builder);
    }

    public static void toXContent(CatchNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        builder.field("block");
        toXContent(irNode.getBlockNode(), builder);
        end(irNode, builder);
    }

    public static void toXContent(ClassNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getFieldsNodes().isEmpty() == false) {
            builder.startArray("fields");
            for (FieldNode field : irNode.getFieldsNodes()) {
                toXContent(field, builder);
            }
            builder.endArray();
        }

        if (irNode.getFunctionsNodes().isEmpty() == false) {
            builder.startArray("functions");
            for (FunctionNode function : irNode.getFunctionsNodes()) {
                toXContent(function, builder);
            }
            builder.endArray();
        }
        builder.field("clinitBlock");
        toXContent(irNode.getClinitBlockNode(), builder);
        end(irNode, builder);
    }

    public static void toXContent(ComparisonNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((BinaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(ConditionalNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);

        if (irNode.getConditionNode() != null) {
            builder.field("condition");
            toXContent(irNode.getConditionNode(), builder);
        }

        toXContentAbstract((BinaryNode) irNode, builder);

        end(irNode, builder);
    }

    public static void toXContent(ConstantNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(ContinueNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(DeclarationBlockNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getDeclarationsNodes().isEmpty() == false) {
            builder.startArray("declarations");
            for (DeclarationNode declaration : irNode.getDeclarationsNodes()) {
                toXContent(declaration, builder);
            }
            builder.endArray();
        }
        end(irNode, builder);
    }

    public static void toXContent(DeclarationNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getExpressionNode() == null) {
            builder.field("expression");
            toXContent(irNode.getExpressionNode(), builder);
        }
        end(irNode, builder);
    }

    public static void toXContent(DefInterfaceReferenceNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(DoWhileLoopNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((ConditionNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(DupNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(ElvisNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((BinaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(FieldNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(FlipArrayIndexNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContent((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(FlipCollectionIndexNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContent((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(FlipDefIndexNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContent((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(ForEachLoopNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getConditionNode() != null) {
            builder.field("condition");
            toXContent(irNode.getConditionNode(), builder);
        }
        end(irNode, builder);
    }

    public static void toXContent(ForEachSubArrayNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((ConditionNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(ForEachSubIterableNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((ConditionNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(ForLoopNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getInitializerNode() != null) {
            builder.startArray("initializer");
            toXContent(irNode.getInitializerNode(), builder);
            builder.endArray();
        }

        if (irNode.getConditionNode() != null) {
            builder.startArray("condition");
            toXContent(irNode.getConditionNode(), builder);
            builder.endArray();
        }

        if (irNode.getAfterthoughtNode() != null) {
            builder.startArray("afterthought");
            toXContent(irNode.getAfterthoughtNode(), builder);
            builder.endArray();
        }

        if (irNode.getBlockNode() != null) {
            builder.field("block");
            toXContent(irNode.getBlockNode(), builder);
        }

        end(irNode, builder);
    }

    public static void toXContent(FunctionNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        builder.field("block");
        toXContent(irNode.getBlockNode(), builder);
        end(irNode, builder);
    }

    public static void toXContent(IfElseNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((ConditionNode) irNode, builder);

        if (irNode.getElseBlockNode() != null) {
            builder.field("else");
            toXContent(irNode.getElseBlockNode(), builder);
        }
        end(irNode, builder);
    }

    public static void toXContent(IfNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((ConditionNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(InstanceofNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContent((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(InvokeCallDefNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((ArgumentsNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(InvokeCallMemberNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((ArgumentsNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(InvokeCallNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getMethod() != null) {
            builder.field("method");
            DecorationToXContent.toXContent(irNode.getMethod(), builder);
        }
        if (irNode.getBox() != null) {
            builder.field("box", irNode.getBox().getSimpleName());
        }
        toXContentAbstract((ArgumentsNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(ListInitializationNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((ArgumentsNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(LoadBraceDefNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(LoadBraceNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(LoadDotArrayLengthNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(LoadDotDefNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(LoadDotNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(LoadDotShortcutNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(LoadFieldMemberNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(LoadListShortcutNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(LoadMapShortcutNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(LoadVariableNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(MapInitializationNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getKeyNodes().isEmpty() == false) {
            builder.startArray("keys");
            for (ExpressionNode expression : irNode.getKeyNodes()) {
                // TODO(stu): shouldn't have to upcast this
                toXContent((IRNode) expression, builder);
            }
            builder.endArray();
        }

        if (irNode.getValueNodes().isEmpty() == false) {
            builder.startArray("values");
            for (ExpressionNode expression : irNode.getValueNodes()) {
                // TODO(stu): shouldn't have to upcast this
                toXContent((IRNode) expression, builder);
            }
            builder.endArray();
        }
        end(irNode, builder);
    }

    public static void toXContent(NewArrayNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((ArgumentsNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(NewObjectNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((ArgumentsNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(NullNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(NullSafeSubNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(ReturnNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getExpressionNode() != null) {
            builder.field("expression");
            toXContent(irNode.getExpressionNode(), builder);
        }
        end(irNode, builder);
    }

    public static void toXContent(StatementExpressionNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getExpressionNode() != null) {
            builder.field("expression");
            // TODO(stu): shouldn't have to upcast this
            toXContent((IRNode) irNode.getExpressionNode(), builder);
        }
        end(irNode, builder);
    }

    public static void toXContent(StaticNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(StoreBraceDefNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(StoreBraceNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(StoreDotDefNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(StoreDotNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(StoreDotShortcutNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(StoreFieldMemberNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(StoreListShortcutNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(StoreMapShortcutNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(StoreVariableNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(StringConcatenationNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((ArgumentsNode) irNode, builder);
        end(irNode, builder);
    }

    public static void toXContent(ThrowNode irNode, XContentBuilderWrapper builder) {
        // TODO(stu): implement
        start(irNode, builder);
        if (irNode.getExpressionNode() != null) {
            builder.field("expression");
            // TODO(stu): check this upcast
            toXContent((IRNode) irNode.getExpressionNode(), builder);
        }
        end(irNode, builder);
    }

    public static void toXContent(TryNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        if (irNode.getBlockNode() != null) {
            builder.field("block");
            toXContent(irNode.getBlockNode(), builder);
        }
        if (irNode.getCatchNodes().isEmpty() == false) {
            builder.startArray("catches");
            for (CatchNode catchNode : irNode.getCatchNodes()) {
                toXContent(catchNode, builder);
            }
            builder.endArray();
        }
        end(irNode, builder);
    }

    public static void toXContent(TypedCaptureReferenceNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(TypedInterfaceReferenceNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(UnaryMathNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((UnaryNode) irNode, builder);
        end(irNode, builder);
    }

   public static void toXContent(WhileLoopNode irNode, XContentBuilderWrapper builder) {
        start(irNode, builder);
        toXContentAbstract((ConditionNode) irNode, builder);
        end(irNode, builder);
    }


    // abstracts
    private static void toXContentAbstract(ArgumentsNode irNode, XContentBuilderWrapper builder) {
        if (irNode.getArgumentNodes().isEmpty() == false) {
            builder.startArray("arguments");
            for (ExpressionNode expression : irNode.getArgumentNodes()) {
                toXContent((IRNode) expression, builder);
            }
            builder.endArray();
        }
    }

    private static void toXContentAbstract(BinaryNode irNode, XContentBuilderWrapper builder) {
        builder.startArray(Fields.LEFT);
        toXContent(irNode.getLeftNode(), builder);
        builder.endArray();

        builder.startArray(Fields.RIGHT);
        toXContent(irNode.getRightNode(), builder);
        builder.endArray();
    }

    private static void toXContentAbstract(ConditionNode irNode, XContentBuilderWrapper builder) {
        if (irNode.getConditionNode() != null) {
            builder.field("condition");
            toXContent(irNode.getConditionNode(), builder);
        }

        if (irNode.getBlockNode() != null) {
            builder.field("block");
            toXContent(irNode.getBlockNode(), builder);
        }
    }

    private static void toXContentAbstract(UnaryNode irNode, XContentBuilderWrapper builder) {
        if (irNode.getChildNode() != null) {
            builder.field("child");
            toXContent(irNode.getChildNode(), builder);
        }
    }

    private static void toXContentAbstract(ExpressionNode irNode, XContentBuilderWrapper builder) {
        // TODO(stu): this should never be used, all expression nodes should be handled by their child class version
        throw new IllegalStateException("should be implemented in child [" + irNode.getClass().getSimpleName() + "]");
    }

    public static void toXContent(StatementNode irNode, XContentBuilderWrapper builder) {
        terminal(irNode, builder);
    }

    public static void toXContent(IRNode irNode, XContentBuilderWrapper builder) {
        if (irNode instanceof BinaryMathNode) {
            toXContent((BinaryMathNode) irNode, builder);
        } else if (irNode instanceof BinaryImplNode) {
            toXContent((BinaryImplNode) irNode, builder);
        } else if (irNode instanceof BlockNode) {
            toXContent((BlockNode) irNode, builder);
        } else if (irNode instanceof BooleanNode) {
            toXContent((BooleanNode) irNode, builder);
        } else if (irNode instanceof BreakNode) {
            toXContent((BreakNode) irNode, builder);
        } else if (irNode instanceof CastNode) {
            toXContent((CastNode) irNode, builder);
        } else if (irNode instanceof CatchNode) {
            toXContent((CatchNode) irNode, builder);
        } else if (irNode instanceof ClassNode) {
            toXContent((ClassNode) irNode, builder);
        } else if (irNode instanceof ComparisonNode) {
            toXContent((ComparisonNode) irNode, builder);
        } else if (irNode instanceof ConditionalNode) {
            toXContent((ConditionalNode) irNode, builder);
        } else if (irNode instanceof ConstantNode) {
            toXContent((ConstantNode) irNode, builder);
        } else if (irNode instanceof ContinueNode) {
            toXContent((ContinueNode) irNode, builder);
        } else if (irNode instanceof DeclarationBlockNode) {
            toXContent((DeclarationBlockNode) irNode, builder);
        } else if (irNode instanceof DeclarationNode) {
            toXContent((DeclarationNode) irNode, builder);
        } else if (irNode instanceof DefInterfaceReferenceNode) {
            toXContent((DefInterfaceReferenceNode) irNode, builder);
        } else if (irNode instanceof DoWhileLoopNode) {
            toXContent((DoWhileLoopNode) irNode, builder);
        } else if (irNode instanceof DupNode) {
            toXContent((DupNode) irNode, builder);
        } else if (irNode instanceof ElvisNode) {
            toXContent((ElvisNode) irNode, builder);
        } else if (irNode instanceof FieldNode) {
            toXContent((FieldNode) irNode, builder);
        } else if (irNode instanceof FlipArrayIndexNode) {
            toXContent((FlipArrayIndexNode) irNode, builder);
        } else if (irNode instanceof FlipCollectionIndexNode) {
            toXContent((FlipCollectionIndexNode) irNode, builder);
        } else if (irNode instanceof FlipDefIndexNode) {
            toXContent((FlipDefIndexNode) irNode, builder);
        } else if (irNode instanceof ForEachLoopNode) {
            toXContent((ForEachLoopNode) irNode, builder);
        } else if (irNode instanceof ForEachSubArrayNode) {
            toXContent((ForEachSubArrayNode) irNode, builder);
        } else if (irNode instanceof ForEachSubIterableNode) {
            toXContent((ForEachSubIterableNode) irNode, builder);
        } else if (irNode instanceof ForLoopNode) {
            toXContent((ForLoopNode) irNode, builder);
        } else if (irNode instanceof FunctionNode) {
            toXContent((FunctionNode) irNode, builder);
        } else if (irNode instanceof IfElseNode) {
            toXContent((IfElseNode) irNode, builder);
        } else if (irNode instanceof IfNode) {
            toXContent((IfNode) irNode, builder);
        } else if (irNode instanceof InstanceofNode) {
            toXContent((InstanceofNode) irNode, builder);
        } else if (irNode instanceof InvokeCallDefNode) {
            toXContent((InvokeCallDefNode) irNode, builder);
        } else if (irNode instanceof InvokeCallMemberNode) {
            toXContent((InvokeCallMemberNode) irNode, builder);
        } else if (irNode instanceof InvokeCallNode) {
            toXContent((InvokeCallNode) irNode, builder);
        } else if (irNode instanceof ListInitializationNode) {
            toXContent((ListInitializationNode) irNode, builder);
        } else if (irNode instanceof LoadBraceDefNode) {
            toXContent((LoadBraceDefNode) irNode, builder);
        } else if (irNode instanceof LoadBraceNode) {
            toXContent((LoadBraceNode) irNode, builder);
        } else if (irNode instanceof LoadDotArrayLengthNode) {
            toXContent((LoadDotArrayLengthNode) irNode, builder);
        } else if (irNode instanceof LoadDotDefNode) {
            toXContent((LoadDotDefNode) irNode, builder);
        } else if (irNode instanceof LoadDotNode) {
            toXContent((LoadDotNode) irNode, builder);
        } else if (irNode instanceof LoadDotShortcutNode) {
            toXContent((LoadDotShortcutNode) irNode, builder);
        } else if (irNode instanceof LoadFieldMemberNode) {
            toXContent((LoadFieldMemberNode) irNode, builder);
        } else if (irNode instanceof LoadListShortcutNode) {
            toXContent((LoadListShortcutNode) irNode, builder);
        } else if (irNode instanceof LoadMapShortcutNode) {
            toXContent((LoadMapShortcutNode) irNode, builder);
        } else if (irNode instanceof LoadVariableNode) {
            toXContent((LoadVariableNode) irNode, builder);
        } else if (irNode instanceof MapInitializationNode) {
            toXContent((MapInitializationNode) irNode, builder);
        } else if (irNode instanceof NewArrayNode) {
            toXContent((NewArrayNode) irNode, builder);
        } else if (irNode instanceof NewObjectNode) {
            toXContent((NewObjectNode) irNode, builder);
        } else if (irNode instanceof NullNode) {
            toXContent((NullNode) irNode, builder);
        } else if (irNode instanceof NullSafeSubNode) {
            toXContent((NullSafeSubNode) irNode, builder);
        } else if (irNode instanceof ReturnNode) {
            toXContent((ReturnNode) irNode, builder);
        } else if (irNode instanceof StatementExpressionNode) {
            toXContent((StatementExpressionNode) irNode, builder);
        } else if (irNode instanceof StaticNode) {
            toXContent((StaticNode) irNode, builder);
        } else if (irNode instanceof StoreBraceDefNode) {
            toXContent((StoreBraceDefNode) irNode, builder);
        } else if (irNode instanceof StoreBraceNode) {
            toXContent((StoreBraceNode) irNode, builder);
        } else if (irNode instanceof StoreDotDefNode) {
            toXContent((StoreDotDefNode) irNode, builder);
        } else if (irNode instanceof StoreDotNode) {
            toXContent((StoreDotNode) irNode, builder);
        } else if (irNode instanceof StoreDotShortcutNode) {
            toXContent((StoreDotShortcutNode) irNode, builder);
        } else if (irNode instanceof StoreFieldMemberNode) {
            toXContent((StoreFieldMemberNode) irNode, builder);
        } else if (irNode instanceof StoreListShortcutNode) {
            toXContent((StoreListShortcutNode) irNode, builder);
        } else if (irNode instanceof StoreMapShortcutNode) {
            toXContent((StoreMapShortcutNode) irNode, builder);
        } else if (irNode instanceof StoreVariableNode) {
            toXContent((StoreVariableNode) irNode, builder);
        } else if (irNode instanceof StringConcatenationNode) {
            toXContent((StringConcatenationNode) irNode, builder);
        } else if (irNode instanceof ThrowNode) {
            toXContent((ThrowNode) irNode, builder);
        } else if (irNode instanceof TryNode) {
            toXContent((TryNode) irNode, builder);
        } else if (irNode instanceof TypedCaptureReferenceNode) {
            toXContent((TypedCaptureReferenceNode) irNode, builder);
        } else if (irNode instanceof TypedInterfaceReferenceNode) {
            toXContent((TypedInterfaceReferenceNode) irNode, builder);
        } else if (irNode instanceof UnaryMathNode) {
            toXContent((UnaryMathNode) irNode, builder);
        } else if (irNode instanceof WhileLoopNode) {
            toXContent((WhileLoopNode) irNode, builder);
        } else if (irNode instanceof ArgumentsNode) { // abstracts
            toXContentAbstract((ArgumentsNode) irNode, builder);
        } else if (irNode instanceof BinaryNode) {
            start(irNode, builder);
            toXContentAbstract((BinaryNode) irNode, builder);
            end(irNode, builder);
        } else if (irNode instanceof ConditionNode) {
            toXContent((ConditionNode) irNode, builder);
        } else if (irNode instanceof UnaryNode) {
            toXContent((UnaryNode) irNode, builder);
        } else if (irNode instanceof ExpressionNode) {
            toXContent((ExpressionNode) irNode, builder);
        } else if (irNode instanceof StatementNode) {
            toXContent((StatementNode) irNode, builder);
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
        List<IRNode.IRDecoration<?>> decorations = irNode.getAllDecorations().stream().sorted(new Comparator<IRNode.IRDecoration<?>>() {
            @Override
            public int compare(IRNode.IRDecoration<?> o1, IRNode.IRDecoration<?> o2) {
                return o1.getClass().getSimpleName().compareTo(o2.getClass().getSimpleName());
            }
        }).collect(Collectors.toList());

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
