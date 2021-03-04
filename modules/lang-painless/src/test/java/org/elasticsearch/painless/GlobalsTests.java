/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless;

import org.elasticsearch.painless.spi.Whitelist;
import org.elasticsearch.script.ScriptContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GlobalsTests extends ScriptTestCase {

    /**
     * Script contexts used to build the script engine. Override to customize which script contexts are available.
     */
    @Override
    protected Map<ScriptContext<?>, List<Whitelist>> scriptContexts() {
        Map<ScriptContext<?>, List<Whitelist>> contexts = super.scriptContexts();
        contexts.put(MockGlobalTestScript.CONTEXT, new ArrayList<>(Whitelist.BASE_WHITELISTS));

        return contexts;
    }

    public abstract static class MockGlobalTestScript {

        private final Map<String, Object> params;
        private final int other;

        public MockGlobalTestScript(Map<String, Object> params, int other) {
            this.params = params;
            this.other = other;
        }

        /** Return the parameters for this script. */
        public Map<String, Object> getParams() {
            return params;
        }

        public int getOther() { return other; }

        public abstract Object execute(double foo, String bar, Map<String, Object> baz);

        public interface Factory {
            MockGlobalTestScript newInstance(Map<String, Object> params, int other);
        }

        public static final String[] PARAMETERS = {"foo", "bar", "baz"};
        public static final ScriptContext<MockGlobalTestScript.Factory> CONTEXT =
            new ScriptContext<>("painless_test_global", MockGlobalTestScript.Factory.class);
    }

    public Object exec(String script, Map<String, Object> vars, int other, double foo, String bar, Map<String, Object> baz) {
        // test actual script execution
        MockGlobalTestScript.Factory factory = scriptEngine.compile(null, script, MockGlobalTestScript.CONTEXT, Collections.emptyMap());
        MockGlobalTestScript testScript = factory.newInstance(vars == null ? Collections.emptyMap() : vars, other);
        return testScript.execute(foo, bar, baz);
    }

    public void testGlobals() {
        Object result = exec("def bar() { params['foo'] } bar(params)", Map.of("foo", 124), 99, 1.5, "barbar", Map.of("abc", true, "dore", "me"));
        assertEquals(124, result);
    }
}
