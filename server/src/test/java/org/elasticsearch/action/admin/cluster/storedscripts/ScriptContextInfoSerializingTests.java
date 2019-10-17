/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.action.admin.cluster.storedscripts;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.ScriptContextInfo;
import org.elasticsearch.script.ScriptContextInfo.ScriptMethodInfo;
import org.elasticsearch.test.AbstractSerializingTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScriptContextInfoSerializingTests extends AbstractSerializingTestCase<ScriptContextInfo> {
    private static int minLength = 1;
    private static int maxLength = 16;

    @Override
    protected ScriptContextInfo doParseInstance(XContentParser parser) throws IOException {
        return ScriptContextInfo.fromXContent(parser);
    }

    @Override
    protected ScriptContextInfo createTestInstance() {
        int numGetters = randomIntBetween(0, maxLength);
        return new ScriptContextInfo(
            randomAlphaOfLengthBetween(minLength, maxLength),
            ScriptMethodInfoSerializingTests.createRandomTestInstance("execute", 0, 0, randomIntBetween(minLength, maxLength)),
            Collections.unmodifiableList(Stream.iterate(0, i -> i < numGetters, i -> i + 1).map(
                (i) -> ScriptMethodInfoSerializingTests.createRandomTestInstance("get", 1, 16, 0)
            ).collect(Collectors.toList()))
        );
    }

    @Override
    protected Writeable.Reader<ScriptContextInfo> instanceReader() { return ScriptContextInfo::new; }


    @Override
    protected ScriptContextInfo mutateInstance(ScriptContextInfo instance) throws IOException {
        // TODO(stu): keep some stuff
        switch (randomIntBetween(0, 2)) {
            case 0:
                return createRandomTestInstanceExcept("", instance.execute, instance.getters);
            case 1:
                return createRandomTestInstanceExcept(instance.name, null, instance.getters);
            default:
                return createRandomTestInstanceExcept(instance.name, instance.execute, new ArrayList<>());
        }
    }

    static ScriptContextInfo createRandomTestInstanceExcept(String exceptName, ScriptMethodInfo execute, List<ScriptMethodInfo> getters) {
        ScriptMethodInfo newExecute = execute != null ?
            ScriptMethodInfoSerializingTests.createRandomTestInstanceExcept("", execute.returnType, execute.parameters,
                () -> "execute"):
            ScriptMethodInfoSerializingTests.createRandomTestInstance("execute", 0, 0, randomIntBetween(minLength, maxLength));
        return new ScriptContextInfo(
            randomValueOtherThan(exceptName, () -> randomAlphaOfLengthBetween(minLength, maxLength)),
            execute,
            ScriptMethodInfoSerializingTests.createRandomTestInstancesExcept(getters, () -> "get" + randomIntBetween(minLength, maxLength))
        );
    }
}
