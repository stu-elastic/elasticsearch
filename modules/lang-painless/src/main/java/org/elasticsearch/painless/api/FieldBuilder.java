/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless.api;

import org.elasticsearch.index.fielddata.ScriptDocValues;

import java.util.Map;

public class FieldBuilder {
    private Map<String, ScriptDocValues<?>> doc;
    public FieldBuilder(Map<String, ScriptDocValues<?>> doc) {
        this.doc = doc;
    }

    public Field field(String name) {
        return null;
    }
}
