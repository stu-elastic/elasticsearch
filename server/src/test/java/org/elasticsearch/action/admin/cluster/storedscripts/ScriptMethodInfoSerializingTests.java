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
import org.elasticsearch.script.ScriptContextInfo.ScriptMethodInfo;
import org.elasticsearch.script.ScriptContextInfo.ScriptMethodInfo.ParameterInfo;
import org.elasticsearch.test.AbstractSerializingTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ScriptMethodInfoSerializingTests extends AbstractSerializingTestCase<ScriptMethodInfo> {
    private static int minLength = 1;
    private static int maxLength = 16;

    @Override
    protected ScriptMethodInfo doParseInstance(XContentParser parser) throws IOException {
        return ScriptMethodInfo.fromXContent(parser);
    }

    @Override
    protected ScriptMethodInfo createTestInstance() {
        return createRandomTestInstance(maxLength);
    }

    @Override
    protected Writeable.Reader<ScriptMethodInfo> instanceReader() { return ScriptMethodInfo::new; }

    @Override
    protected ScriptMethodInfo mutateInstance(ScriptMethodInfo instance) throws IOException {
        // TODO(stu): keep some stuff
        switch (randomIntBetween(0, 3)) {
            case 0:
                return createRandomTestInstanceExcept("", instance.returnType, new ArrayList<>());
            case 1:
                return createRandomTestInstanceExcept(instance.name, "", new ArrayList<>());
            case 2:
                return createRandomTestInstanceExcept(instance.name, instance.returnType, new ArrayList<>());
            default:
                return createRandomTestInstanceExcept(instance.name, instance.returnType, instance.parameters);
        }
    }

    static ScriptMethodInfo withName(String name, ScriptMethodInfo instance) {
        return new ScriptMethodInfo(name, instance.returnType, instance.parameters);
    }

    static ScriptMethodInfo createRandomTestInstance(int maxParameterLength) {
        return new ScriptMethodInfo(
            randomAlphaOfLengthBetween(minLength, maxLength),
            randomAlphaOfLengthBetween(minLength, maxLength),
            ScriptParameterInfoSerializingTests.createRandomTestInstances(maxParameterLength)
        );
    }

    static ScriptMethodInfo createRandomTestInstance(String prefix, int minSuffix, int maxSuffix, int maxParameterLength) {
        return new ScriptMethodInfo(
            prefix + randomAlphaOfLengthBetween(minSuffix, maxSuffix),
            randomAlphaOfLengthBetween(minLength, maxLength),
            ScriptParameterInfoSerializingTests.createRandomTestInstances(maxParameterLength)
        );
    }

    static ScriptMethodInfo createRandomTestInstanceExcept(String exceptName, String exceptReturnType, List<ParameterInfo> parameters) {
        return createRandomTestInstanceExcept(exceptName, exceptReturnType, parameters,
            () -> randomAlphaOfLengthBetween(minLength, maxLength));
    }

    static ScriptMethodInfo createRandomTestInstanceExcept(String exceptName, String exceptReturnType, List<ParameterInfo> parameters,
        Supplier<String> nameGenerator) {
        return new ScriptMethodInfo(
            randomValueOtherThan(exceptName, nameGenerator),
            randomValueOtherThan(exceptReturnType, () -> randomAlphaOfLengthBetween(minLength, maxLength)),
            ScriptParameterInfoSerializingTests.createRandomTestInstancesExcept(new HashSet<>(parameters))
        );
    }

    static List<ScriptMethodInfo> createRandomTestInstancesExcept(List<ScriptMethodInfo> except, Supplier<String> nameGenerator) {
        int numInstances = randomValueOtherThan(except.size(), () -> randomIntBetween(0, maxLength));
        Set<String> exceptNames = except.stream().map(e -> e.name).collect(Collectors.toSet());
        Set<String> exceptReturnTypes = except.stream().map(e -> e.returnType).collect(Collectors.toSet());
        Set<ParameterInfo> exceptParams = except.stream().flatMap(e -> e.parameters.stream()).collect(Collectors.toSet());
        List<ScriptMethodInfo> instances = new ArrayList<>(numInstances);
        for (int i = 0; i < numInstances; i++) {
            instances.add(new ScriptMethodInfo(
                randomValueOtherThanMany(exceptNames::contains, nameGenerator),
                randomValueOtherThanMany(exceptReturnTypes::contains, () -> randomAlphaOfLengthBetween(minLength, maxLength)),
                ScriptParameterInfoSerializingTests.createRandomTestInstancesExcept(exceptParams)
            ));
        }
        return Collections.unmodifiableList(instances);
    }
}
