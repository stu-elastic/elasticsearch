/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless;

import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.painless.action.PainlessExecuteAction;
import org.elasticsearch.script.LongFieldScript;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptFactory;
import org.elasticsearch.search.lookup.SearchLookup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class TestFieldScript {
    private List<Long> values = new ArrayList<>();

    @SuppressWarnings("unused")
    public static final String[] PARAMETERS = {};
    public interface Factory {
        TestFieldScript newInstance();
    }

    public static final ScriptContext<TestFieldScript.Factory> CONTEXT =
            new ScriptContext<>("painless_test_fieldscript", TestFieldScript.Factory.class);

    public static class Emit {
        private final TestFieldScript script;

        public Emit(TestFieldScript script) {
            this.script = script;
        }

        public void emit(long v) {
            script.emit(v);
        }
    }

    public abstract Object execute();

    public final void emit(long v) {
        values.add(v);
    }

    public long[] fetchValues() {
        return values.stream().mapToLong(i->i).toArray();
    }
}
