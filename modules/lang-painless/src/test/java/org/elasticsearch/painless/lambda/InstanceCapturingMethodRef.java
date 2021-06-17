/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless.lambda;

import java.util.ArrayList;
import java.util.List;

public class InstanceCapturingMethodRef {
    private final int multi;

    public InstanceCapturingMethodRef(int multi) {
        this.multi = multi;
    }

    public int getMulti() {
        return multi;
    }

    public static void main(String[] args) {
        List<Integer> l = new ArrayList<>();
        l.add(1);
        l.add(-100);
        l.add(100);
        InstanceCapturingMethodRef icmf = new InstanceCapturingMethodRef(-1);
        icmf.myListCompare(l);
    }

    int myCompare(int x, int y) {
        return getMulti() * (x - y);
    }

    void myListCompare(List<Integer> l) {
        l.sort(this::myCompare);
    }
}
