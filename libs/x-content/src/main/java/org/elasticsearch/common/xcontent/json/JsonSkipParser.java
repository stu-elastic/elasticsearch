/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.xcontent.json;

import java.nio.charset.StandardCharsets;

public class JsonSkipParser {
    private static final byte WS_SPACE = " ".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte WS_TAB = "\t".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte WS_CR = "\r".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte WS_LF = "\n".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte O_OPEN = "{".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte O_CLOSE = "}".getBytes(StandardCharsets.UTF_8)[0];
    private static final byte QUOTE = "\"".getBytes(StandardCharsets.UTF_8)[0];

    enum Mode {
        PROLOGUE, EPILOGUE,
        PRE_KEY, KEY, POST_KEY,
        PRE_VALUE, VALUE, POST_VALUE,
    }

    enum Value {
        OBJECT, ARRAY, STRING, TRUE, FALSE, NULL
    }

    private static int nextToken(byte[] b, int i, byte expected) {
        while (i < b.length) {
            if (b[i] == WS_SPACE || b[i] == WS_TAB || b[i] == WS_CR || b[i] == WS_LF) {
                i++;
            } else if (b[i] == expected) {
                return i + 1;
            }
            throw new IllegalStateException("Expected [" + expected + "] but found [" + b[i] + "] at [" + i +"]");
        }
        throw new IllegalStateException("Expected [" + expected + "] but stream ended at [" + i +"]");
    }

    public static Object get(byte[] b, String key) {
        byte[] kb = key.getBytes(StandardCharsets.UTF_8);
        int i = nextToken(b, 0, O_OPEN);
        i = nextToken(b, i, QUOTE);

        while (b[i] == WS_SPACE || b[i] == WS_TAB || b[i] == WS_CR || b[i] == WS_LF) {
            if (i < b.length-1) {
                i++;
            } else {

            }
        }
        if (b[i] == O_OPEN) {
            i++;
        }

            switch (mode) {
                case PROLOGUE:
                case PRE_KEY:
                    if (b[i] == WS_SPACE || b[i] == WS_TAB || b[i] == WS_CR || b[i] == WS_LF) {
                    } else if (b[i] == QUOTE) {
                        mode = Mode.KEY;
                    } else {
                    }
                }
            }
        }
        return null;
    }
}
