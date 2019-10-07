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

package org.elasticsearch.script;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ScriptGetterInfo {
    public final String name, returnType;

    ScriptGetterInfo(String name, String returnType) {
        this.name = name;
        this.returnType = returnType;
    }

    static List<ScriptGetterInfo> ScriptGetterInfos(Class<?> instanceClazz) {
        ArrayList<ScriptGetterInfo> getters = new ArrayList<>();
        for (java.lang.reflect.Method m: instanceClazz.getMethods()) {
            if (!m.isDefault() &&
                m.getName().startsWith("get") &&
                !m.getName().equals("getClass") &&
                !Modifier.isStatic(m.getModifiers()) &&
                m.getParameters().length == 0) {
                getters.add(new ScriptGetterInfo(m.getName(), m.getReturnType().getTypeName()));
            }
        }
        return getters;
    }
}
