package org.elasticsearch.script;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.MockNode;
import org.elasticsearch.plugins.Plugin;

import java.util.Map;

public class MockScriptService extends ScriptService {
    /**
     * Marker plugin used by {@link MockNode} to enable {@link MockScriptService}.
     */
    public static class TestPlugin extends Plugin {}

    public MockScriptService(Settings settings, Map<String, ScriptEngine> engines, Map<String, ScriptContext<?>> contexts) {
        super(settings, engines, contexts);
    }

    @Override
    boolean compilationLimitsEnabled() {
        return false;
    }
}
