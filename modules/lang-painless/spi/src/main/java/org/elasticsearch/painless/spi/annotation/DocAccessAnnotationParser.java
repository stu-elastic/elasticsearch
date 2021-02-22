/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless.spi.annotation;

import java.util.Map;

public class DocAccessAnnotationParser implements WhitelistAnnotationParser {
    public static final DocAccessAnnotationParser INSTANCE = new DocAccessAnnotationParser();
    private DocAccessAnnotationParser() {}

    @Override
    public Object parse(Map<String, String> arguments) {
        if (arguments.isEmpty() == false) {
            throw new IllegalArgumentException("unexpected parameters for [@doc_access] annotation, found " + arguments);
        }

        return INSTANCE;
    }
}
