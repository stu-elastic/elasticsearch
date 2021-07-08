/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.script.expression;

import org.apache.lucene.expressions.Expression;
import org.apache.lucene.expressions.SimpleBindings;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.elasticsearch.script.Field;
import org.elasticsearch.script.FieldScript;
import org.elasticsearch.script.GeneralScriptException;

import java.io.IOException;
import java.util.stream.Stream;

public class ExpressionFieldScript implements FieldScript.LeafFactory {
    private final Expression exprScript;
    private final DoubleValuesSource source;

    ExpressionFieldScript(Expression e, SimpleBindings b) {
        this.exprScript = e;
        this.source = exprScript.getDoubleValuesSource(b);
    }

    @Override
    public FieldScript newInstance(final LeafReaderContext leaf) throws IOException {
        return new FieldScript() {

            @Override
            public Field<?> field(String fieldName) {
                return null;
            }

            @Override
            public Stream<Field<?>> fields(String fieldGlob) {
                return null;
            }

            // Fake the scorer until setScorer is called.
            DoubleValues values = source.getValues(leaf, null);

            @Override
            public Object execute() {
                try {
                    return values.doubleValue();
                } catch (Exception exception) {
                    throw new GeneralScriptException("Error evaluating " + exprScript, exception);
                }
            }

            @Override
            public void setDocument(int d) {
                try {
                    values.advanceExact(d);
                } catch (IOException e) {
                    throw new IllegalStateException("Can't advance to doc using " + exprScript, e);
                }
            }
        };
    }
}
