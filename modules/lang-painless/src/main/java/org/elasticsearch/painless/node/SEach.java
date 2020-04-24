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

package org.elasticsearch.painless.node;

import org.elasticsearch.painless.AnalyzerCaster;
import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.ir.BlockNode;
import org.elasticsearch.painless.ir.ConditionNode;
import org.elasticsearch.painless.ir.ExpressionNode;
import org.elasticsearch.painless.ir.ForEachLoopNode;
import org.elasticsearch.painless.ir.ForEachSubArrayNode;
import org.elasticsearch.painless.ir.ForEachSubIterableNode;
import org.elasticsearch.painless.ir.IRNode;
import org.elasticsearch.painless.lookup.PainlessCast;
import org.elasticsearch.painless.lookup.PainlessLookupUtility;
import org.elasticsearch.painless.lookup.PainlessMethod;
import org.elasticsearch.painless.lookup.def;
import org.elasticsearch.painless.phase.DefaultIRTreeBuilderPhase;
import org.elasticsearch.painless.phase.DefaultSemanticAnalysisPhase;
import org.elasticsearch.painless.phase.UserTreeVisitor;
import org.elasticsearch.painless.symbol.Decorations.AnyContinue;
import org.elasticsearch.painless.symbol.Decorations.BeginLoop;
import org.elasticsearch.painless.symbol.Decorations.ExpressionPainlessCast;
import org.elasticsearch.painless.symbol.Decorations.InLoop;
import org.elasticsearch.painless.symbol.Decorations.IterablePainlessMethod;
import org.elasticsearch.painless.symbol.Decorations.LoopEscape;
import org.elasticsearch.painless.symbol.Decorations.Read;
import org.elasticsearch.painless.symbol.Decorations.SemanticVariable;
import org.elasticsearch.painless.symbol.Decorations.ValueType;
import org.elasticsearch.painless.symbol.ScriptScope;
import org.elasticsearch.painless.symbol.SemanticScope;
import org.elasticsearch.painless.symbol.SemanticScope.Variable;

import java.util.Iterator;
import java.util.Objects;

import static org.elasticsearch.painless.lookup.PainlessLookupUtility.typeToCanonicalTypeName;

/**
 * Represents a for-each loop and defers to subnodes depending on type.
 */
public class SEach extends AStatement {

    private final String canonicalTypeName;
    private final String symbol;
    private final AExpression iterableNode;
    private final SBlock blockNode;

    public SEach(int identifier, Location location, String canonicalTypeName, String symbol, AExpression iterableNode, SBlock blockNode) {
        super(identifier, location);

        this.canonicalTypeName = Objects.requireNonNull(canonicalTypeName);
        this.symbol = Objects.requireNonNull(symbol);
        this.iterableNode = Objects.requireNonNull(iterableNode);
        this.blockNode = blockNode;
    }

    public String getCanonicalTypeName() {
        return canonicalTypeName;
    }

    public String getSymbol() {
        return symbol;
    }

    public AExpression getIterableNode() {
        return iterableNode;
    }

    public SBlock getBlockNode() {
        return blockNode;
    }

    @Override
    public <Input, Output> Output visit(UserTreeVisitor<Input, Output> userTreeVisitor, Input input) {
        return userTreeVisitor.visitEach(this, input);
    }

    public static void visitDefaultSemanticAnalysis(
            DefaultSemanticAnalysisPhase visitor, SEach userEachNode, SemanticScope semanticScope) {

        AExpression userIterableNode = userEachNode.getIterableNode();
        semanticScope.setCondition(userIterableNode, Read.class);
        visitor.checkedVisit(userIterableNode, semanticScope);

        String canonicalTypeName = userEachNode.getCanonicalTypeName();
        Class<?> type = semanticScope.getScriptScope().getPainlessLookup().canonicalTypeNameToType(canonicalTypeName);

        if (type == null) {
            throw userEachNode.createError(new IllegalArgumentException(
                    "invalid foreach loop: type [" + canonicalTypeName + "] not found"));
        }

        semanticScope = semanticScope.newLocalScope();

        Location location = userEachNode.getLocation();
        String symbol = userEachNode.getSymbol();
        Variable variable = semanticScope.defineVariable(location, type, symbol, true);
        semanticScope.putDecoration(userEachNode, new SemanticVariable(variable));

        SBlock userBlockNode = userEachNode.getBlockNode();

        if (userBlockNode == null) {
            throw userEachNode.createError(new IllegalArgumentException("extraneous foreach loop"));
        }

        semanticScope.setCondition(userBlockNode, BeginLoop.class);
        semanticScope.setCondition(userBlockNode, InLoop.class);
        visitor.visit(userBlockNode, semanticScope);

        if (semanticScope.getCondition(userBlockNode, LoopEscape.class) &&
                semanticScope.getCondition(userBlockNode, AnyContinue.class) == false) {
            throw userEachNode.createError(new IllegalArgumentException("extraneous foreach loop"));
        }

        Class<?> iterableValueType = semanticScope.getDecoration(userIterableNode, ValueType.class).getValueType();

        if (iterableValueType.isArray()) {
            PainlessCast painlessCast =
                    AnalyzerCaster.getLegalCast(location, iterableValueType.getComponentType(), variable.getType(), true, true);

            if (painlessCast != null) {
                semanticScope.putDecoration(userEachNode, new ExpressionPainlessCast(painlessCast));
            }
        } else if (iterableValueType == def.class || Iterable.class.isAssignableFrom(iterableValueType)) {
            if (iterableValueType != def.class) {
                PainlessMethod method = semanticScope.getScriptScope().getPainlessLookup().
                        lookupPainlessMethod(iterableValueType, false, "iterator", 0);

                if (method == null) {
                    throw userEachNode.createError(new IllegalArgumentException("invalid foreach loop: " +
                            "method [" + typeToCanonicalTypeName(iterableValueType) + ", iterator/0] not found"));
                }

                semanticScope.putDecoration(userEachNode, new IterablePainlessMethod(method));
            }

            PainlessCast painlessCast = AnalyzerCaster.getLegalCast(location, def.class, type, true, true);

            if (painlessCast != null) {
                semanticScope.putDecoration(userEachNode, new ExpressionPainlessCast(painlessCast));
            }
        } else {
            throw userEachNode.createError(new IllegalArgumentException("invalid foreach loop: " +
                    "cannot iterate over type [" + PainlessLookupUtility.typeToCanonicalTypeName(iterableValueType) + "]."));
        }
    }

    public static IRNode visitDefaultIRTreeBuild(DefaultIRTreeBuilderPhase visitor, SEach userEachNode, ScriptScope scriptScope) {
        Variable variable = scriptScope.getDecoration(userEachNode, SemanticVariable.class).getSemanticVariable();
        PainlessCast painlessCast = scriptScope.hasDecoration(userEachNode, ExpressionPainlessCast.class) ?
                scriptScope.getDecoration(userEachNode, ExpressionPainlessCast.class).getExpressionPainlessCast() : null;
        ExpressionNode irIterableNode = (ExpressionNode)visitor.visit(userEachNode.getIterableNode(), scriptScope);
        Class<?> iterableValueType = scriptScope.getDecoration(userEachNode.getIterableNode(), ValueType.class).getValueType();
        BlockNode irBlockNode = (BlockNode)visitor.visit(userEachNode.getBlockNode(), scriptScope);

        ConditionNode irConditionNode;

        if (iterableValueType.isArray()) {
            ForEachSubArrayNode irForEachSubArrayNode = new ForEachSubArrayNode();
            irForEachSubArrayNode.setConditionNode(irIterableNode);
            irForEachSubArrayNode.setBlockNode(irBlockNode);
            irForEachSubArrayNode.setLocation(userEachNode.getLocation());
            irForEachSubArrayNode.setVariableType(variable.getType());
            irForEachSubArrayNode.setVariableName(variable.getName());
            irForEachSubArrayNode.setCast(painlessCast);
            irForEachSubArrayNode.setArrayType(iterableValueType);
            irForEachSubArrayNode.setArrayName("#array" + userEachNode.getLocation().getOffset());
            irForEachSubArrayNode.setIndexType(int.class);
            irForEachSubArrayNode.setIndexName("#index" + userEachNode.getLocation().getOffset());
            irForEachSubArrayNode.setIndexedType(iterableValueType.getComponentType());
            irForEachSubArrayNode.setContinuous(false);
            irConditionNode = irForEachSubArrayNode;
        } else if (iterableValueType == def.class || Iterable.class.isAssignableFrom(iterableValueType)) {
            ForEachSubIterableNode irForEachSubIterableNode = new ForEachSubIterableNode();
            irForEachSubIterableNode.setConditionNode(irIterableNode);
            irForEachSubIterableNode.setBlockNode(irBlockNode);
            irForEachSubIterableNode.setLocation(userEachNode.getLocation());
            irForEachSubIterableNode.setVariableType(variable.getType());
            irForEachSubIterableNode.setVariableName(variable.getName());
            irForEachSubIterableNode.setCast(painlessCast);
            irForEachSubIterableNode.setIteratorType(Iterator.class);
            irForEachSubIterableNode.setIteratorName("#itr" + userEachNode.getLocation().getOffset());
            irForEachSubIterableNode.setMethod(iterableValueType == def.class ? null :
                    scriptScope.getDecoration(userEachNode, IterablePainlessMethod.class).getIterablePainlessMethod());
            irForEachSubIterableNode.setContinuous(false);
            irConditionNode = irForEachSubIterableNode;
        } else {
            throw userEachNode.createError(new IllegalStateException("illegal tree structure"));
        }

        ForEachLoopNode irForEachLoopNode = new ForEachLoopNode();
        irForEachLoopNode.setConditionNode(irConditionNode);
        irForEachLoopNode.setLocation(userEachNode.getLocation());

        return irForEachLoopNode;
    }
}
