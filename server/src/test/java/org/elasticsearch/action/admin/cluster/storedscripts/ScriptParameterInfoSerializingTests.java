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
import org.elasticsearch.script.ScriptContextInfo.ScriptMethodInfo.ParameterInfo;
import org.elasticsearch.test.AbstractSerializingTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ScriptParameterInfoSerializingTests extends AbstractSerializingTestCase<ParameterInfo> {
    private static int minLength = 1;
    private static int maxLength = 16;

    @Override
    protected ParameterInfo doParseInstance(XContentParser parser) throws IOException {
        return ParameterInfo.fromXContent(parser);
    }

    @Override
    protected ParameterInfo createTestInstance() {
        return createRandomTestInstance();
    }

    @Override
    protected Writeable.Reader<ParameterInfo> instanceReader() {
        return ParameterInfo::new;
    }

    @Override
    protected ParameterInfo mutateInstance(ParameterInfo instance) throws IOException {
        // TODO(stu): keep some stuff
        switch (randomIntBetween(0, 2)) {
            case 0:
                return createRandomTestInstanceExcept("", instance.name);
            case 1:
                return createRandomTestInstanceExcept(instance.type, "");
            default:
                return createRandomTestInstanceExcept(instance.type, instance.name);
        }
    }

    static ParameterInfo createRandomTestInstance() {
        return new ParameterInfo(randomAlphaOfLengthBetween(minLength, maxLength), randomAlphaOfLengthBetween(minLength, maxLength));
    }

    static ParameterInfo createRandomTestInstanceExcept(String exceptType, String exceptName) {
        return new ParameterInfo(
            randomValueOtherThan(exceptType, () -> randomAlphaOfLengthBetween(minLength, maxLength)),
            randomValueOtherThan(exceptName, () -> randomAlphaOfLengthBetween(minLength, maxLength))
        );
    }

    static List<ParameterInfo> createRandomTestInstances(int maxLength) {
        int numInstances = randomIntBetween(0, maxLength);
        List<ParameterInfo> instances = new ArrayList<>(numInstances);
        for (int i = 0; i < numInstances; i++) {
            instances.add(createRandomTestInstance());
        }
        return Collections.unmodifiableList(instances);
    }

    static List<ParameterInfo> createRandomTestInstancesExcept(Set<ParameterInfo> except) {
        int numInstances = randomValueOtherThan(except.size(), () -> randomIntBetween(0, maxLength));
        Set<String> exceptTypes = except.stream().map(e -> e.type).collect(Collectors.toSet());
        Set<String> exceptNames = except.stream().map(e -> e.name).collect(Collectors.toSet());
        List<ParameterInfo> instances = new ArrayList<>(numInstances);
        for (int i = 0; i < numInstances; i++) {
            instances.add(new ParameterInfo(
                randomValueOtherThanMany(exceptTypes::contains, () -> randomAlphaOfLengthBetween(minLength, maxLength)),
                randomValueOtherThanMany(exceptNames::contains, () -> randomAlphaOfLengthBetween(minLength, maxLength))
            ));
        }
        return Collections.unmodifiableList(instances);
    }
}
