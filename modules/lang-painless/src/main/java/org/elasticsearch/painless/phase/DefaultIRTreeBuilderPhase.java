/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.painless.phase;

import org.elasticsearch.painless.ir.CastNode;
import org.elasticsearch.painless.ir.ExpressionNode;
import org.elasticsearch.painless.ir.IRNode;
import org.elasticsearch.painless.node.AExpression;
import org.elasticsearch.painless.node.EAssignment;
import org.elasticsearch.painless.node.EBinary;
import org.elasticsearch.painless.node.EBool;
import org.elasticsearch.painless.node.EBoolean;
import org.elasticsearch.painless.node.EBrace;
import org.elasticsearch.painless.node.ECall;
import org.elasticsearch.painless.node.ECallLocal;
import org.elasticsearch.painless.node.EComp;
import org.elasticsearch.painless.node.EConditional;
import org.elasticsearch.painless.node.EDecimal;
import org.elasticsearch.painless.node.EDot;
import org.elasticsearch.painless.node.EElvis;
import org.elasticsearch.painless.node.EExplicit;
import org.elasticsearch.painless.node.EFunctionRef;
import org.elasticsearch.painless.node.EInstanceof;
import org.elasticsearch.painless.node.ELambda;
import org.elasticsearch.painless.node.EListInit;
import org.elasticsearch.painless.node.EMapInit;
import org.elasticsearch.painless.node.ENewArray;
import org.elasticsearch.painless.node.ENewArrayFunctionRef;
import org.elasticsearch.painless.node.ENewObj;
import org.elasticsearch.painless.node.ENull;
import org.elasticsearch.painless.node.ENumeric;
import org.elasticsearch.painless.node.ERegex;
import org.elasticsearch.painless.node.EString;
import org.elasticsearch.painless.node.ESymbol;
import org.elasticsearch.painless.node.EUnary;
import org.elasticsearch.painless.node.SBlock;
import org.elasticsearch.painless.node.SBreak;
import org.elasticsearch.painless.node.SCatch;
import org.elasticsearch.painless.node.SClass;
import org.elasticsearch.painless.node.SContinue;
import org.elasticsearch.painless.node.SDeclBlock;
import org.elasticsearch.painless.node.SDeclaration;
import org.elasticsearch.painless.node.SDo;
import org.elasticsearch.painless.node.SEach;
import org.elasticsearch.painless.node.SExpression;
import org.elasticsearch.painless.node.SFor;
import org.elasticsearch.painless.node.SFunction;
import org.elasticsearch.painless.node.SIf;
import org.elasticsearch.painless.node.SIfElse;
import org.elasticsearch.painless.node.SReturn;
import org.elasticsearch.painless.node.SThrow;
import org.elasticsearch.painless.node.STry;
import org.elasticsearch.painless.node.SWhile;
import org.elasticsearch.painless.symbol.Decorations.ExpressionPainlessCast;
import org.elasticsearch.painless.symbol.ScriptScope;

public class DefaultIRTreeBuilderPhase implements UserTreeVisitor<ScriptScope, IRNode> {

    public ExpressionNode injectCast(AExpression userExpressionNode, ScriptScope scriptScope) {
        ExpressionNode irExpressionNode = (ExpressionNode)visit(userExpressionNode, scriptScope);

        if (irExpressionNode == null) {
            return null;
        }

        ExpressionPainlessCast expressionPainlessCast = scriptScope.getDecoration(userExpressionNode, ExpressionPainlessCast.class);

        if (expressionPainlessCast == null) {
            return irExpressionNode;
        }

        CastNode castNode = new CastNode();
        castNode.setLocation(irExpressionNode.getLocation());
        castNode.setExpressionType(expressionPainlessCast.getExpressionPainlessCast().targetType);
        castNode.setCast(expressionPainlessCast.getExpressionPainlessCast());
        castNode.setChildNode(irExpressionNode);

        return castNode;
    }

    @Override
    public IRNode visitClass(SClass userClassNode, ScriptScope scriptScope) {
        return SClass.visitDefaultIRTreeBuild(this, userClassNode, scriptScope);
    }

    @Override
    public IRNode visitFunction(SFunction userFunctionNode, ScriptScope scriptScope) {
        return SFunction.visitDefaultIRTreeBuild(this, userFunctionNode, scriptScope);
    }

    @Override
    public IRNode visitBlock(SBlock userBlockNode, ScriptScope scriptScope) {
        return SBlock.visitDefaultIRTreeBuild(this, userBlockNode, scriptScope);
    }

    @Override
    public IRNode visitIf(SIf userIfNode, ScriptScope scriptScope) {
        return SIf.visitDefaultIRTreeBuild(this, userIfNode, scriptScope);
    }

    @Override
    public IRNode visitIfElse(SIfElse userIfElseNode, ScriptScope scriptScope) {
        return SIfElse.visitDefaultIRTreeBuild(this, userIfElseNode, scriptScope);
    }

    @Override
    public IRNode visitWhile(SWhile userWhileNode, ScriptScope scriptScope) {
        return SWhile.visitDefaultIRTreeBuild(this, userWhileNode, scriptScope);
    }

    @Override
    public IRNode visitDo(SDo userDoNode, ScriptScope scriptScope) {
        return SDo.visitDefaultIRTreeBuild(this, userDoNode, scriptScope);
    }

    @Override
    public IRNode visitFor(SFor userForNode, ScriptScope scriptScope) {
        return SFor.visitDefaultIRTreeBuild(this, userForNode, scriptScope);
    }

    @Override
    public IRNode visitEach(SEach userEachNode, ScriptScope scriptScope) {
        return SEach.visitDefaultIRTreeBuild(this, userEachNode, scriptScope);
    }

    @Override
    public IRNode visitDeclBlock(SDeclBlock userDeclBlockNode, ScriptScope scriptScope) {
        return SDeclBlock.visitDefaultIRTreeBuild(this, userDeclBlockNode, scriptScope);
    }

    @Override
    public IRNode visitDeclaration(SDeclaration userDeclarationNode, ScriptScope scriptScope) {
        return SDeclaration.visitDefaultIRTreeBuild(this, userDeclarationNode, scriptScope);
    }

    @Override
    public IRNode visitReturn(SReturn userReturnNode, ScriptScope scriptScope) {
        return SReturn.visitDefaultIRTreeBuild(this, userReturnNode, scriptScope);
    }

    @Override
    public IRNode visitExpression(SExpression userExpressionNode, ScriptScope scriptScope) {
        return SExpression.visitDefaultIRTreeBuild(this, userExpressionNode, scriptScope);
    }

    @Override
    public IRNode visitTry(STry userTryNode, ScriptScope scriptScope) {
        return STry.visitDefaultIRTreeBuild(this, userTryNode, scriptScope);
    }

    @Override
    public IRNode visitCatch(SCatch userCatchNode, ScriptScope scriptScope) {
        return SCatch.visitDefaultIRTreeBuild(this, userCatchNode, scriptScope);
    }

    @Override
    public IRNode visitThrow(SThrow userThrowNode, ScriptScope scriptScope) {
        return SThrow.visitDefaultIRTreeBuild(this, userThrowNode, scriptScope);
    }

    @Override
    public IRNode visitContinue(SContinue userContinueNode, ScriptScope scriptScope) {
        return SContinue.visitDefaultIRTreeBuild(this, userContinueNode, scriptScope);
    }

    @Override
    public IRNode visitBreak(SBreak userBreakNode, ScriptScope scriptScope) {
        return SBreak.visitDefaultIRTreeBuild(this, userBreakNode, scriptScope);
    }

    @Override
    public IRNode visitAssignment(EAssignment userAssignmentNode, ScriptScope scriptScope) {
        return EAssignment.visitDefaultIRTreeBuild(this, userAssignmentNode, scriptScope);
    }

    @Override
    public IRNode visitUnary(EUnary userUnaryNode, ScriptScope scriptScope) {
        return EUnary.visitDefaultIRTreeBuild(this, userUnaryNode, scriptScope);
    }

    @Override
    public IRNode visitBinary(EBinary userBinaryNode, ScriptScope scriptScope) {
        return EBinary.visitDefaultIRTreeBuild(this, userBinaryNode, scriptScope);
    }

    @Override
    public IRNode visitBool(EBool userBoolNode, ScriptScope scriptScope) {
        return EBool.visitDefaultIRTreeBuild(this, userBoolNode, scriptScope);
    }

    @Override
    public IRNode visitComp(EComp userCompNode, ScriptScope scriptScope) {
        return EComp.visitDefaultIRTreeBuild(this, userCompNode, scriptScope);
    }

    @Override
    public IRNode visitExplicit(EExplicit userExplicitNode, ScriptScope scriptScope) {
        return injectCast(userExplicitNode.getChildNode(), scriptScope);
    }

    @Override
    public IRNode visitInstanceof(EInstanceof userInstanceofNode, ScriptScope scriptScope) {
        return EInstanceof.visitDefaultIRTreeBuild(this, userInstanceofNode, scriptScope);
    }

    @Override
    public IRNode visitConditional(EConditional userConditionalNode, ScriptScope scriptScope) {
        return EConditional.visitDefaultIRTreeBuild(this, userConditionalNode, scriptScope);
    }

    @Override
    public IRNode visitElvis(EElvis userElvisNode, ScriptScope scriptScope) {
        return EElvis.visitDefaultIRTreeBuild(this, userElvisNode, scriptScope);
    }

    @Override
    public IRNode visitListInit(EListInit userListInitNode, ScriptScope scriptScope) {
        return EListInit.visitDefaultIRTreeBuild(this, userListInitNode, scriptScope);
    }

    @Override
    public IRNode visitMapInit(EMapInit userMapInitNode, ScriptScope scriptScope) {
        return EMapInit.visitDefaultIRTreeBuild(this, userMapInitNode, scriptScope);
    }

    @Override
    public IRNode visitNewArray(ENewArray userNewArrayNode, ScriptScope scriptScope) {
        return ENewArray.visitDefaultIRTreeBuild(this, userNewArrayNode, scriptScope);
    }

    @Override
    public IRNode visitNewObj(ENewObj userNewObjectNode, ScriptScope scriptScope) {
        return ENewObj.visitDefaultIRTreeBuild(this, userNewObjectNode, scriptScope);
    }

    @Override
    public IRNode visitCallLocal(ECallLocal callLocalNode, ScriptScope scriptScope) {
        return ECallLocal.visitDefaultIRTreeBuild(this, callLocalNode, scriptScope);
    }

    @Override
    public IRNode visitBoolean(EBoolean userBooleanNode, ScriptScope scriptScope) {
        return EBoolean.visitDefaultIRTreeBuild(this, userBooleanNode, scriptScope);
    }
    
    @Override
    public IRNode visitNumeric(ENumeric userNumericNode, ScriptScope scriptScope) {
        return ENumeric.visitDefaultIRTreeBuild(this, userNumericNode, scriptScope);
    }
    
    @Override
    public IRNode visitDecimal(EDecimal userDecimalNode, ScriptScope scriptScope) {
        return EDecimal.visitDefaultIRTreeBuild(this, userDecimalNode, scriptScope);
    }
    
    @Override
    public IRNode visitString(EString userStringNode, ScriptScope scriptScope) {
        return EString.visitDefaultIRTreeBuild(this, userStringNode, scriptScope);
    }

    @Override
    public IRNode visitNull(ENull userNullNode, ScriptScope scriptScope) {
        return ENull.visitDefaultIRTreeBuild(this, userNullNode, scriptScope);
    }

    @Override
    public IRNode visitRegex(ERegex userRegexNode, ScriptScope scriptScope) {
        return ERegex.visitDefaultIRTreeBuild(this, userRegexNode, scriptScope);
    }

    @Override
    public IRNode visitLambda(ELambda userLambdaNode, ScriptScope scriptScope) {
        return ELambda.visitDefaultIRTreeBuild(this, userLambdaNode, scriptScope);
    }

    @Override
    public IRNode visitFunctionRef(EFunctionRef userFunctionRefNode, ScriptScope scriptScope) {
        return EFunctionRef.visitDefaultIRTreeBuild(this, userFunctionRefNode, scriptScope);
    }

    @Override
    public IRNode visitNewArrayFunctionRef(ENewArrayFunctionRef userNewArrayFunctionRefNode, ScriptScope scriptScope) {
        return ENewArrayFunctionRef.visitDefaultIRTreeBuild(this, userNewArrayFunctionRefNode, scriptScope);
    }

    @Override
    public IRNode visitSymbol(ESymbol userSymbolNode, ScriptScope scriptScope) {
        return ESymbol.visitDefaultIRTreeBuild(this, userSymbolNode, scriptScope);
    }

    @Override
    public IRNode visitDot(EDot userDotNode, ScriptScope scriptScope) {
        return EDot.visitDefaultIRTreeBuild(this, userDotNode, scriptScope);
    }

    @Override
    public IRNode visitBrace(EBrace userBraceNode, ScriptScope scriptScope) {
        return EBrace.visitDefaultIRTreeBuild(this, userBraceNode, scriptScope);
    }

    @Override
    public IRNode visitCall(ECall userCallNode, ScriptScope scriptScope) {
        return ECall.visitDefaultIRTreeBuild(this, userCallNode, scriptScope);
    }
}
