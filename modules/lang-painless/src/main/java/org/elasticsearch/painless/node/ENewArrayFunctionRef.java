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

import org.elasticsearch.painless.FunctionRef;
import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.ir.BlockNode;
import org.elasticsearch.painless.ir.DefInterfaceReferenceNode;
import org.elasticsearch.painless.ir.FunctionNode;
import org.elasticsearch.painless.ir.IRNode;
import org.elasticsearch.painless.ir.NewArrayNode;
import org.elasticsearch.painless.ir.ReferenceNode;
import org.elasticsearch.painless.ir.ReturnNode;
import org.elasticsearch.painless.ir.TypedInterfaceReferenceNode;
import org.elasticsearch.painless.ir.LoadVariableNode;
import org.elasticsearch.painless.phase.DefaultIRTreeBuilderPhase;
import org.elasticsearch.painless.phase.DefaultSemanticAnalysisPhase;
import org.elasticsearch.painless.phase.UserTreeVisitor;
import org.elasticsearch.painless.symbol.Decorations.EncodingDecoration;
import org.elasticsearch.painless.symbol.Decorations.MethodNameDecoration;
import org.elasticsearch.painless.symbol.Decorations.Read;
import org.elasticsearch.painless.symbol.Decorations.ReferenceDecoration;
import org.elasticsearch.painless.symbol.Decorations.ReturnType;
import org.elasticsearch.painless.symbol.Decorations.TargetType;
import org.elasticsearch.painless.symbol.Decorations.ValueType;
import org.elasticsearch.painless.symbol.Decorations.Write;
import org.elasticsearch.painless.symbol.ScriptScope;
import org.elasticsearch.painless.symbol.SemanticScope;

import java.util.Collections;
import java.util.Objects;

/**
 * Represents a function reference.
 */
public class ENewArrayFunctionRef extends AExpression {

    private final String canonicalTypeName;

    public ENewArrayFunctionRef(int identifier, Location location, String canonicalTypeName) {
        super(identifier, location);

        this.canonicalTypeName = Objects.requireNonNull(canonicalTypeName);
    }

    public String getCanonicalTypeName() {
        return canonicalTypeName;
    }

    @Override
    public <Input, Output> Output visit(UserTreeVisitor<Input, Output> userTreeVisitor, Input input) {
        return userTreeVisitor.visitNewArrayFunctionRef(this, input);
    }

    public static void visitDefaultSemanticAnalysis(
            DefaultSemanticAnalysisPhase visitor, ENewArrayFunctionRef userNewArrayFunctionRefNode, SemanticScope semanticScope) {

        String canonicalTypeName = userNewArrayFunctionRefNode.getCanonicalTypeName();

        if (semanticScope.getCondition(userNewArrayFunctionRefNode, Write.class)) {
            throw userNewArrayFunctionRefNode.createError(new IllegalArgumentException(
                    "cannot assign a value to new array function reference with target type [ + " + canonicalTypeName  + "]"));
        }

        if (semanticScope.getCondition(userNewArrayFunctionRefNode, Read.class) == false) {
            throw userNewArrayFunctionRefNode.createError(new IllegalArgumentException(
                    "not a statement: new array function reference with target type [" + canonicalTypeName + "] not used"));
        }

        ScriptScope scriptScope = semanticScope.getScriptScope();
        TargetType targetType = semanticScope.getDecoration(userNewArrayFunctionRefNode, TargetType.class);

        Class<?> valueType;
        Class<?> clazz = scriptScope.getPainlessLookup().canonicalTypeNameToType(canonicalTypeName);
        semanticScope.putDecoration(userNewArrayFunctionRefNode, new ReturnType(clazz));

        if (clazz == null) {
            throw userNewArrayFunctionRefNode.createError(new IllegalArgumentException("Not a type [" + canonicalTypeName + "]."));
        }

        String name = scriptScope.getNextSyntheticName("newarray");
        scriptScope.getFunctionTable().addFunction(name, clazz, Collections.singletonList(int.class), true, true);
        semanticScope.putDecoration(userNewArrayFunctionRefNode, new MethodNameDecoration(name));

        if (targetType == null) {
            String defReferenceEncoding = "Sthis." + name + ",0";
            valueType = String.class;
            scriptScope.putDecoration(userNewArrayFunctionRefNode, new EncodingDecoration(defReferenceEncoding));
        } else {
            FunctionRef ref = FunctionRef.create(scriptScope.getPainlessLookup(), scriptScope.getFunctionTable(),
                    userNewArrayFunctionRefNode.getLocation(), targetType.getTargetType(), "this", name, 0);
            valueType = targetType.getTargetType();
            semanticScope.putDecoration(userNewArrayFunctionRefNode, new ReferenceDecoration(ref));
        }

        semanticScope.putDecoration(userNewArrayFunctionRefNode, new ValueType(valueType));
    }

    public static IRNode visitDefaultIRTreeBuild(
            DefaultIRTreeBuilderPhase visitor, ENewArrayFunctionRef userNewArrayFunctionRefNode, ScriptScope scriptScope) {

        ReferenceNode irReferenceNode;

        if (scriptScope.hasDecoration(userNewArrayFunctionRefNode, TargetType.class)) {
            TypedInterfaceReferenceNode typedInterfaceReferenceNode = new TypedInterfaceReferenceNode();
            typedInterfaceReferenceNode.setReference(
                    scriptScope.getDecoration(userNewArrayFunctionRefNode, ReferenceDecoration.class).getReference());
            irReferenceNode = typedInterfaceReferenceNode;
        } else {
            DefInterfaceReferenceNode defInterfaceReferenceNode = new DefInterfaceReferenceNode();
            defInterfaceReferenceNode.setDefReferenceEncoding(
                    scriptScope.getDecoration(userNewArrayFunctionRefNode, EncodingDecoration.class).getEncoding());
            irReferenceNode = defInterfaceReferenceNode;
        }

        Class<?> returnType = scriptScope.getDecoration(userNewArrayFunctionRefNode, ReturnType.class).getReturnType();

        LoadVariableNode irLoadVariableNode = new LoadVariableNode();
        irLoadVariableNode.setLocation(userNewArrayFunctionRefNode.getLocation());
        irLoadVariableNode.setExpressionType(int.class);
        irLoadVariableNode.setName("size");

        NewArrayNode irNewArrayNode = new NewArrayNode();
        irNewArrayNode.setLocation(userNewArrayFunctionRefNode.getLocation());
        irNewArrayNode.setExpressionType(returnType);
        irNewArrayNode.setInitialize(false);

        irNewArrayNode.addArgumentNode(irLoadVariableNode);

        ReturnNode irReturnNode = new ReturnNode();
        irReturnNode.setLocation(userNewArrayFunctionRefNode.getLocation());
        irReturnNode.setExpressionNode(irNewArrayNode);

        BlockNode irBlockNode = new BlockNode();
        irBlockNode.setAllEscape(true);
        irBlockNode.setStatementCount(1);
        irBlockNode.addStatementNode(irReturnNode);

        FunctionNode irFunctionNode = new FunctionNode();
        irFunctionNode.setMaxLoopCounter(0);
        irFunctionNode.setName(scriptScope.getDecoration(userNewArrayFunctionRefNode, MethodNameDecoration.class).getMethodName());
        irFunctionNode.setReturnType(returnType);
        irFunctionNode.addTypeParameter(int.class);
        irFunctionNode.addParameterName("size");
        irFunctionNode.setStatic(true);
        irFunctionNode.setVarArgs(false);
        irFunctionNode.setSynthetic(true);
        irFunctionNode.setBlockNode(irBlockNode);

        scriptScope.getIRClassNode().addFunctionNode(irFunctionNode);

        irReferenceNode.setLocation(userNewArrayFunctionRefNode.getLocation());
        irReferenceNode.setExpressionType(scriptScope.getDecoration(userNewArrayFunctionRefNode, ValueType.class).getValueType());

        return irReferenceNode;
    }
}
