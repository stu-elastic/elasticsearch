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

package org.elasticsearch.painless;

public class RegexChecker {
    public static class Match {
        int start, end;
        String orig;
        Match(int start, int end, String orig) {
            this.start = start;
            this.end = end;
            this.orig = orig;
        }

        @Override
        public String toString() {
            return "start: [" + start + "], end: [" + end + "], match: [" + orig.substring(start, end+1) + "], " +
                "original: [" + orig + "] " +
                "context: [" + orig.substring(0, start) +
                           "**" + orig.substring(start, end+1) + "**" +
                          orig.substring(end+1) + "]";
        }
    }

    public static Match checkBackReferences(String regex) {
        boolean escape = false;
        int start = 0;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '\\') {
                if (escape ^= true) {
                    start = i;
                }
            } else if (escape) {
                if (c >= '0' && c <= '9') {
                    return new Match(start, i, regex);
                } else if (c == 'k') {
                    // \k must always be followed by <name>, so just capture \k
                    return new Match(start, i, regex);
                } else {
                    escape = false;
                }
            }
        }
        return null;
    }
}
