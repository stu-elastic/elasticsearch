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

import org.elasticsearch.painless.action.PainlessContextClassInfo;
import org.elasticsearch.painless.action.PainlessContextInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.painless.ContextWhitelistProcessor.PainlessCommon;
import static org.elasticsearch.painless.ContextWhitelistProcessor.getContextInfos;
import static org.elasticsearch.painless.ContextWhitelistProcessor.getDisplayNames;

public class ContextApiJsonWriter {
    public static void main(String[] args) throws IOException {
        List<PainlessContextInfo> contextInfos = getContextInfos(System.getProperty("cluster.uri"));
        PainlessCommon common = new PainlessCommon(contextInfos);
        List<PainlessContextClassInfo> classInfos = ContextWhitelistProcessor.sortClassInfos(
            common.classes,
            new ArrayList<>(sharedClassInfos)
        );
        Map<String, String> javaNamesToDisplayNames = getDisplayNames(contextInfos);
    }
}
