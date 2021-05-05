/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless.symbol;

import java.util.Objects;

/**
 * Encoding of method references for def lookup, see {@code Def.lookupMethod}.
 */
public class DefReferenceEncoding {
    private final boolean stronglyTyped;
    private final String type;
    private final String method;
    private final int captures;

    public DefReferenceEncoding(boolean stronglyTyped, String type, String method, int captures) {
        this.stronglyTyped = stronglyTyped;
        this.type = Objects.requireNonNull(type);
        this.method = Objects.requireNonNull(method);
        this.captures = captures;
    }

    public DefReferenceEncoding withMethod(String method) {
        return new DefReferenceEncoding(this.stronglyTyped, this.type, method, this.captures);
    }

    public String getType() {
        return type;
    }

    public String getMethod() {
        return method;
    }

    public String encoded() {
        return (stronglyTyped ? "S" : "D") + type + "." + method + "," + captures;
    }
}
