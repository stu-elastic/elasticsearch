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
import org.elasticsearch.painless.Scope;
import org.elasticsearch.painless.ir.BlockNode;
import org.elasticsearch.painless.ir.ClassNode;
import org.elasticsearch.painless.ir.DoWhileLoopNode;
import org.elasticsearch.painless.lookup.PainlessCast;
import org.elasticsearch.painless.symbol.ScriptRoot;

import java.util.Objects;

/**
 * Represents a do-while loop.
 */
public class SDo extends AStatement {

    protected final SBlock block;
    protected final AExpression condition;

    public SDo(Location location, SBlock block, AExpression condition) {
        super(location);

        this.condition = Objects.requireNonNull(condition);
        this.block = block;
    }

    @Override
    Output analyze(ClassNode classNode, ScriptRoot scriptRoot, Scope scope, Input input) {
        Output output = new Output();
        scope = scope.newLocalScope();

        if (block == null) {
            throw createError(new IllegalArgumentException("Extraneous do while loop."));
        }

        Input blockInput = new Input();
        blockInput.beginLoop = true;
        blockInput.inLoop = true;
        Output blockOutput = block.analyze(classNode, scriptRoot, scope, blockInput);

        if (blockOutput.loopEscape && blockOutput.anyContinue == false) {
            throw createError(new IllegalArgumentException("Extraneous do while loop."));
        }

        AExpression.Input conditionInput = new AExpression.Input();
        conditionInput.expected = boolean.class;
        AExpression.Output conditionOutput = AExpression.analyze(condition, classNode, scriptRoot, scope, conditionInput);
        PainlessCast conditionCast = AnalyzerCaster.getLegalCast(condition.location,
                conditionOutput.actual, conditionInput.expected, conditionInput.explicit, conditionInput.internal);


        boolean continuous = false;

        if (condition instanceof EBoolean) {
            continuous = ((EBoolean)condition).constant;

            if (!continuous) {
                throw createError(new IllegalArgumentException("Extraneous do while loop."));
            }

            if (blockOutput.anyBreak == false) {
                output.methodEscape = true;
                output.allEscape = true;
            }
        }

        output.statementCount = 1;

        DoWhileLoopNode doWhileLoopNode = new DoWhileLoopNode();
        doWhileLoopNode.setConditionNode(AExpression.cast(conditionOutput.expressionNode, conditionCast));
        doWhileLoopNode.setBlockNode((BlockNode)blockOutput.statementNode);
        doWhileLoopNode.setLocation(location);
        doWhileLoopNode.setContinuous(continuous);

        output.statementNode = doWhileLoopNode;

        return output;
    }
}
