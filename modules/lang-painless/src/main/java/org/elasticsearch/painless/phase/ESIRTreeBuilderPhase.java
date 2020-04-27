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

import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.PainlessError;
import org.elasticsearch.painless.PainlessExplainError;
import org.elasticsearch.painless.ScriptClassInfo;
import org.elasticsearch.painless.ScriptClassInfo.MethodArgument;
import org.elasticsearch.painless.ir.AccessNode;
import org.elasticsearch.painless.ir.BlockNode;
import org.elasticsearch.painless.ir.InvokeCallNode;
import org.elasticsearch.painless.ir.CatchNode;
import org.elasticsearch.painless.ir.ClassNode;
import org.elasticsearch.painless.ir.ConstantNode;
import org.elasticsearch.painless.ir.DeclarationNode;
import org.elasticsearch.painless.ir.ExpressionNode;
import org.elasticsearch.painless.ir.FieldNode;
import org.elasticsearch.painless.ir.FunctionNode;
import org.elasticsearch.painless.ir.IRNode;
import org.elasticsearch.painless.ir.InvokeCallMemberNode;
import org.elasticsearch.painless.ir.LoadFieldMemberNode;
import org.elasticsearch.painless.ir.NullNode;
import org.elasticsearch.painless.ir.ReturnNode;
import org.elasticsearch.painless.ir.StaticNode;
import org.elasticsearch.painless.ir.ThrowNode;
import org.elasticsearch.painless.ir.TryNode;
import org.elasticsearch.painless.ir.LoadVariableNode;
import org.elasticsearch.painless.lookup.PainlessLookup;
import org.elasticsearch.painless.lookup.PainlessMethod;
import org.elasticsearch.painless.node.SFunction;
import org.elasticsearch.painless.symbol.Decorations.MethodEscape;
import org.elasticsearch.painless.symbol.FunctionTable.LocalFunction;
import org.elasticsearch.painless.symbol.ScriptScope;
import org.elasticsearch.script.ScriptException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ESIRTreeBuilderPhase extends DefaultIRTreeBuilderPhase {

    @Override
    public IRNode visitFunction(SFunction userFunctionNode, ScriptScope scriptScope) {
        String functionName = userFunctionNode.getFunctionName();

        // This injects additional ir nodes required for
        // the "execute" method. This includes injection of ir nodes
        // to convert get methods into local variables for those
        // that are used and adds additional sandboxing by wrapping
        // the main "execute" block with several exceptions.
        if ("execute".equals(functionName)) {
            ScriptClassInfo scriptClassInfo = scriptScope.getScriptClassInfo();
            LocalFunction localFunction =
                    scriptScope.getFunctionTable().getFunction(functionName, scriptClassInfo.getExecuteArguments().size());
            Class<?> returnType = localFunction.getReturnType();

            boolean methodEscape = scriptScope.getCondition(userFunctionNode, MethodEscape.class);
            BlockNode irBlockNode = (BlockNode)visit(userFunctionNode.getBlockNode(), scriptScope);

            if (methodEscape == false) {
                ExpressionNode irExpressionNode;

                if (returnType == void.class) {
                    irExpressionNode = null;
                } else {
                    if (returnType.isPrimitive()) {
                        ConstantNode constantNode = new ConstantNode();
                        constantNode.setLocation(userFunctionNode.getLocation());
                        constantNode.setExpressionType(returnType);

                        if (returnType == boolean.class) {
                            constantNode.setConstant(false);
                        } else if (returnType == byte.class
                                || returnType == char.class
                                || returnType == short.class
                                || returnType == int.class) {
                            constantNode.setConstant(0);
                        } else if (returnType == long.class) {
                            constantNode.setConstant(0L);
                        } else if (returnType == float.class) {
                            constantNode.setConstant(0f);
                        } else if (returnType == double.class) {
                            constantNode.setConstant(0d);
                        } else {
                            throw userFunctionNode.createError(new IllegalStateException("illegal tree structure"));
                        }

                        irExpressionNode = constantNode;
                    } else {
                        irExpressionNode = new NullNode();
                        irExpressionNode.setLocation(userFunctionNode.getLocation());
                        irExpressionNode.setExpressionType(returnType);
                    }
                }

                ReturnNode irReturnNode = new ReturnNode();
                irReturnNode.setLocation(userFunctionNode.getLocation());
                irReturnNode.setExpressionNode(irExpressionNode);

                irBlockNode.addStatementNode(irReturnNode);
            }

            List<String> parameterNames = new ArrayList<>(scriptClassInfo.getExecuteArguments().size());

            for (MethodArgument methodArgument : scriptClassInfo.getExecuteArguments()) {
                parameterNames.add(methodArgument.getName());
            }

            FunctionNode irFunctionNode = new FunctionNode();
            irFunctionNode.setBlockNode(irBlockNode);
            irFunctionNode.setLocation(userFunctionNode.getLocation());
            irFunctionNode.setName("execute");
            irFunctionNode.setReturnType(returnType);
            irFunctionNode.getTypeParameters().addAll(localFunction.getTypeParameters());
            irFunctionNode.getParameterNames().addAll(parameterNames);
            irFunctionNode.setStatic(false);
            irFunctionNode.setVarArgs(false);
            irFunctionNode.setSynthetic(false);
            irFunctionNode.setMaxLoopCounter(scriptScope.getCompilerSettings().getMaxLoopCounter());

            injectStaticFieldsAndGetters(scriptScope.getIRClassNode());
            injectGetsDeclarations(irBlockNode, scriptScope);
            injectNeedsMethods(scriptScope);
            injectSandboxExceptions(irFunctionNode);

            return irFunctionNode;
        } else {
            return super.visitFunction(userFunctionNode, scriptScope);
        }
    }

    // adds static fields and getter methods required by PainlessScript for exception handling
    protected void injectStaticFieldsAndGetters(ClassNode classNode) {
        Location internalLocation = new Location("$internal$ScriptInjectionPhase$injectStaticFieldsAndGetters", 0);
        int modifiers = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;

        FieldNode fieldNode = new FieldNode();
        fieldNode.setLocation(internalLocation);
        fieldNode.setModifiers(modifiers);
        fieldNode.setFieldType(String.class);
        fieldNode.setName("$NAME");

        classNode.addFieldNode(fieldNode);

        fieldNode = new FieldNode();
        fieldNode.setLocation(internalLocation);
        fieldNode.setModifiers(modifiers);
        fieldNode.setFieldType(String.class);
        fieldNode.setName("$SOURCE");

        classNode.addFieldNode(fieldNode);

        fieldNode = new FieldNode();
        fieldNode.setLocation(internalLocation);
        fieldNode.setModifiers(modifiers);
        fieldNode.setFieldType(BitSet.class);
        fieldNode.setName("$STATEMENTS");

        classNode.addFieldNode(fieldNode);

        FunctionNode functionNode = new FunctionNode();
        functionNode.setLocation(internalLocation);
        functionNode.setName("getName");
        functionNode.setReturnType(String.class);
        functionNode.setStatic(false);
        functionNode.setVarArgs(false);
        functionNode.setSynthetic(true);
        functionNode.setMaxLoopCounter(0);

        classNode.addFunctionNode(functionNode);

        BlockNode blockNode = new BlockNode();
        blockNode.setLocation(internalLocation);
        blockNode.setAllEscape(true);
        blockNode.setStatementCount(1);

        functionNode.setBlockNode(blockNode);

        ReturnNode returnNode = new ReturnNode();
        returnNode.setLocation(internalLocation);

        blockNode.addStatementNode(returnNode);

        LoadFieldMemberNode loadFieldMemberNode = new LoadFieldMemberNode();
        loadFieldMemberNode.setLocation(internalLocation);
        loadFieldMemberNode.setExpressionType(String.class);
        loadFieldMemberNode.setName("$NAME");
        loadFieldMemberNode.setStatic(true);

        returnNode.setExpressionNode(loadFieldMemberNode);

        functionNode = new FunctionNode();
        functionNode.setLocation(internalLocation);
        functionNode.setName("getSource");
        functionNode.setReturnType(String.class);
        functionNode.setStatic(false);
        functionNode.setVarArgs(false);
        functionNode.setSynthetic(true);
        functionNode.setMaxLoopCounter(0);

        classNode.addFunctionNode(functionNode);

        blockNode = new BlockNode();
        blockNode.setLocation(internalLocation);
        blockNode.setAllEscape(true);
        blockNode.setStatementCount(1);

        functionNode.setBlockNode(blockNode);

        returnNode = new ReturnNode();
        returnNode.setLocation(internalLocation);

        blockNode.addStatementNode(returnNode);

        loadFieldMemberNode = new LoadFieldMemberNode();
        loadFieldMemberNode.setLocation(internalLocation);
        loadFieldMemberNode.setExpressionType(String.class);
        loadFieldMemberNode.setName("$SOURCE");
        loadFieldMemberNode.setStatic(true);

        returnNode.setExpressionNode(loadFieldMemberNode);

        functionNode = new FunctionNode();
        functionNode.setLocation(internalLocation);
        functionNode.setName("getStatements");
        functionNode.setReturnType(BitSet.class);
        functionNode.setStatic(false);
        functionNode.setVarArgs(false);
        functionNode.setSynthetic(true);
        functionNode.setMaxLoopCounter(0);

        classNode.addFunctionNode(functionNode);

        blockNode = new BlockNode();
        blockNode.setLocation(internalLocation);
        blockNode.setAllEscape(true);
        blockNode.setStatementCount(1);

        functionNode.setBlockNode(blockNode);

        returnNode = new ReturnNode();
        returnNode.setLocation(internalLocation);

        blockNode.addStatementNode(returnNode);

        loadFieldMemberNode = new LoadFieldMemberNode();
        loadFieldMemberNode.setLocation(internalLocation);
        loadFieldMemberNode.setExpressionType(BitSet.class);
        loadFieldMemberNode.setName("$STATEMENTS");
        loadFieldMemberNode.setStatic(true);

        returnNode.setExpressionNode(loadFieldMemberNode);
    }

    // convert gets methods to a new set of inserted ir nodes as necessary -
    // requires the gets method name be modified from "getExample" to "example"
    // if a get method variable isn't used it's declaration node is removed from
    // the ir tree permanently so there is no frivolous variable slotting
    protected void injectGetsDeclarations(BlockNode blockNode, ScriptScope scriptScope) {
        Location internalLocation = new Location("$internal$ScriptInjectionPhase$injectGetsDeclarations", 0);

        for (int i = 0; i < scriptScope.getScriptClassInfo().getGetMethods().size(); ++i) {
            Method getMethod = scriptScope.getScriptClassInfo().getGetMethods().get(i);
            String name = getMethod.getName().substring(3);
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);

            if (scriptScope.getUsedVariables().contains(name)) {
                Class<?> returnType = scriptScope.getScriptClassInfo().getGetReturns().get(i);

                DeclarationNode declarationNode = new DeclarationNode();
                declarationNode.setLocation(internalLocation);
                declarationNode.setName(name);
                declarationNode.setDeclarationType(returnType);
                blockNode.getStatementsNodes().add(0, declarationNode);

                InvokeCallMemberNode invokeCallMemberNode = new InvokeCallMemberNode();
                invokeCallMemberNode.setLocation(internalLocation);
                invokeCallMemberNode.setExpressionType(declarationNode.getDeclarationType());
                invokeCallMemberNode.setLocalFunction(new LocalFunction(
                        getMethod.getName(), returnType, Collections.emptyList(), true, false));
                declarationNode.setExpressionNode(invokeCallMemberNode);
            }
        }
    }

    // injects needs methods as defined by ScriptClassInfo
    protected void injectNeedsMethods(ScriptScope scriptScope) {
        ClassNode classNode = scriptScope.getIRClassNode();
        Location internalLocation = new Location("$internal$ScriptInjectionPhase$injectNeedsMethods", 0);

        for (org.objectweb.asm.commons.Method needsMethod : scriptScope.getScriptClassInfo().getNeedsMethods()) {
            String name = needsMethod.getName();
            name = name.substring(5);
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);

            FunctionNode functionNode = new FunctionNode();
            functionNode.setLocation(internalLocation);
            functionNode.setName(needsMethod.getName());
            functionNode.setReturnType(boolean.class);
            functionNode.setStatic(false);
            functionNode.setVarArgs(false);
            functionNode.setSynthetic(true);
            functionNode.setMaxLoopCounter(0);

            classNode.addFunctionNode(functionNode);

            BlockNode blockNode = new BlockNode();
            blockNode.setLocation(internalLocation);
            blockNode.setAllEscape(true);
            blockNode.setStatementCount(1);

            functionNode.setBlockNode(blockNode);

            ReturnNode returnNode = new ReturnNode();
            returnNode.setLocation(internalLocation);

            blockNode.addStatementNode(returnNode);

            ConstantNode constantNode = new ConstantNode();
            constantNode.setLocation(internalLocation);
            constantNode.setExpressionType(boolean.class);
            constantNode.setConstant(scriptScope.getUsedVariables().contains(name));

            returnNode.setExpressionNode(constantNode);
        }
    }

    // decorate the execute method with nodes to wrap the user statements with
    // the sandboxed errors as follows:
    // } catch (PainlessExplainError e) {
    //     throw this.convertToScriptException(e, e.getHeaders($DEFINITION))
    // }
    // and
    // } catch (PainlessError | BootstrapMethodError | OutOfMemoryError | StackOverflowError | Exception e) {
    //     throw this.convertToScriptException(e, e.getHeaders())
    // }
    protected void injectSandboxExceptions(FunctionNode functionNode) {
        try {
            Location internalLocation = new Location("$internal$ScriptInjectionPhase$injectSandboxExceptions", 0);
            BlockNode blockNode = functionNode.getBlockNode();

            TryNode tryNode = new TryNode();
            tryNode.setLocation(internalLocation);
            tryNode.setBlockNode(blockNode);

            CatchNode catchNode = new CatchNode();
            catchNode.setLocation(internalLocation);
            catchNode.setExceptionType(PainlessExplainError.class);
            catchNode.setSymbol("#painlessExplainError");

            tryNode.addCatchNode(catchNode);

            BlockNode catchBlockNode = new BlockNode();
            catchBlockNode.setLocation(internalLocation);
            catchBlockNode.setAllEscape(true);
            catchBlockNode.setStatementCount(1);

            catchNode.setBlockNode(catchBlockNode);

            ThrowNode throwNode = new ThrowNode();
            throwNode.setLocation(internalLocation);

            catchBlockNode.addStatementNode(throwNode);

            InvokeCallMemberNode invokeCallMemberNode = new InvokeCallMemberNode();
            invokeCallMemberNode.setLocation(internalLocation);
            invokeCallMemberNode.setExpressionType(ScriptException.class);
            invokeCallMemberNode.setLocalFunction(new LocalFunction(
                            "convertToScriptException",
                            ScriptException.class,
                            Arrays.asList(Throwable.class, Map.class),
                            true,
                            false
                    )
            );

            throwNode.setExpressionNode(invokeCallMemberNode);

            LoadVariableNode loadVariableNode = new LoadVariableNode();
            loadVariableNode.setLocation(internalLocation);
            loadVariableNode.setExpressionType(ScriptException.class);
            loadVariableNode.setName("#painlessExplainError");

            invokeCallMemberNode.addArgumentNode(loadVariableNode);

            AccessNode irAccessNode = new AccessNode();
            irAccessNode.setLocation(internalLocation);
            irAccessNode.setExpressionType(Map.class);

            invokeCallMemberNode.addArgumentNode(irAccessNode);

            loadVariableNode = new LoadVariableNode();
            loadVariableNode.setLocation(internalLocation);
            loadVariableNode.setExpressionType(PainlessExplainError.class);
            loadVariableNode.setName("#painlessExplainError");

            irAccessNode.setLeftNode(loadVariableNode);

            InvokeCallNode invokeCallNode = new InvokeCallNode();
            invokeCallNode.setLocation(internalLocation);
            invokeCallNode.setExpressionType(Map.class);
            invokeCallNode.setBox(PainlessExplainError.class);
            invokeCallNode.setMethod(new PainlessMethod(
                            PainlessExplainError.class.getMethod(
                                    "getHeaders",
                                    PainlessLookup.class),
                            PainlessExplainError.class,
                            null,
                            Collections.emptyList(),
                            null,
                            null,
                            null
                    )
            );

            irAccessNode.setRightNode(invokeCallNode);

            LoadFieldMemberNode loadFieldMemberNode = new LoadFieldMemberNode();
            loadFieldMemberNode.setLocation(internalLocation);
            loadFieldMemberNode.setExpressionType(PainlessLookup.class);
            loadFieldMemberNode.setName("$DEFINITION");
            loadFieldMemberNode.setStatic(true);

            invokeCallNode.addArgumentNode(loadFieldMemberNode);

            for (Class<?> throwable : new Class<?>[] {
                    PainlessError.class, BootstrapMethodError.class, OutOfMemoryError.class, StackOverflowError.class, Exception.class}) {

                String name = throwable.getSimpleName();
                name = "#" + Character.toLowerCase(name.charAt(0)) + name.substring(1);

                catchNode = new CatchNode();
                catchNode.setLocation(internalLocation);
                catchNode.setExceptionType(throwable);
                catchNode.setSymbol(name);

                tryNode.addCatchNode(catchNode);

                catchBlockNode = new BlockNode();
                catchBlockNode.setLocation(internalLocation);
                catchBlockNode.setAllEscape(true);
                catchBlockNode.setStatementCount(1);

                catchNode.setBlockNode(catchBlockNode);

                throwNode = new ThrowNode();
                throwNode.setLocation(internalLocation);

                catchBlockNode.addStatementNode(throwNode);

                invokeCallMemberNode = new InvokeCallMemberNode();
                invokeCallMemberNode.setLocation(internalLocation);
                invokeCallMemberNode.setExpressionType(ScriptException.class);
                invokeCallMemberNode.setLocalFunction(new LocalFunction(
                                "convertToScriptException",
                                ScriptException.class,
                                Arrays.asList(Throwable.class, Map.class),
                                true,
                                false
                        )
                );

                throwNode.setExpressionNode(invokeCallMemberNode);

                loadVariableNode = new LoadVariableNode();
                loadVariableNode.setLocation(internalLocation);
                loadVariableNode.setExpressionType(ScriptException.class);
                loadVariableNode.setName(name);

                invokeCallMemberNode.addArgumentNode(loadVariableNode);

                irAccessNode = new AccessNode();
                irAccessNode.setLocation(internalLocation);
                irAccessNode.setExpressionType(Map.class);

                invokeCallMemberNode.addArgumentNode(irAccessNode);

                StaticNode staticNode = new StaticNode();
                staticNode.setLocation(internalLocation);
                staticNode.setExpressionType(Collections.class);

                irAccessNode.setLeftNode(staticNode);

                invokeCallNode = new InvokeCallNode();
                invokeCallNode.setLocation(internalLocation);
                invokeCallNode.setExpressionType(Map.class);
                invokeCallNode.setBox(Collections.class);
                invokeCallNode.setMethod(new PainlessMethod(
                                Collections.class.getMethod("emptyMap"),
                                Collections.class,
                                null,
                                Collections.emptyList(),
                                null,
                                null,
                                null
                        )
                );

                irAccessNode.setRightNode(invokeCallNode);
            }

            blockNode = new BlockNode();
            blockNode.setLocation(blockNode.getLocation());
            blockNode.setAllEscape(blockNode.doAllEscape());
            blockNode.setStatementCount(blockNode.getStatementCount());
            blockNode.addStatementNode(tryNode);

            functionNode.setBlockNode(blockNode);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
