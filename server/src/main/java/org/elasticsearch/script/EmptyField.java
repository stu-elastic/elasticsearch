/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.script;

import java.util.Collections;
import java.util.List;

public class EmptyField implements Field<String> {
    protected final String name;

    public EmptyField(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public String getValue(String defaultValue) {
        return defaultValue;
    }

    @Override
    public List<String> getValues() {
        return Collections.emptyList();
    }
}
