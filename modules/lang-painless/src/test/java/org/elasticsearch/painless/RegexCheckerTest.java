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

import junit.framework.TestCase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexCheckerTest extends TestCase {
    public void test() {
        //assertNull(check("abcabc", "abcabcabc"));
        //assertNotNull(check("(abc)\\1", "abcabc0"));
        //assertNotNull(check("(?<foo>abc)\\k<foo>", "abcabck"));
        //assertNull(check("(?<foo>abc)\\k", ""));
        assertNotNull(check("(?:abc)\\1", "abc\\1")); // this compiles and doesn't match anything
        //assertNull(check("(abc)\\\\1", "abc\\1"));
        //assertNotNull(check("(abc)\\\\\\1", "abc\\abc"));
    }

    public RegexChecker.Match check(String regex, String shouldMatch) {
        Pattern p = Pattern.compile(regex);
        if (shouldMatch != null && shouldMatch.length() > 0) {
            Matcher m = p.matcher(shouldMatch);
            assertTrue(m.find());
        }
        return RegexChecker.checkBackReferences(p.pattern());
    }
}
