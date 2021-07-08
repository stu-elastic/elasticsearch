/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.script;

import org.elasticsearch.search.lookup.LeafSearchLookup;

import java.util.stream.Stream;

public class ReadDocValuesFieldProxy implements FieldProxy {
    protected final LeafSearchLookup leafLookup;

    public ReadDocValuesFieldProxy(LeafSearchLookup leafLookup) {
        this.leafLookup = leafLookup;
    }

    @Override
    public Field<?> field(String fieldName) {
        return new EmptyField(fieldName);
    }

    @Override
    public Stream<Field<?>> fields(String fieldGlob) {
        return Stream.empty();
    }

    @Override
    public void setDocument(int docID) {
        leafLookup.setDocument(docID);
    }
}
