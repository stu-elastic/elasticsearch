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

import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.painless.action.PainlessContextClassBindingInfo;
import org.elasticsearch.painless.action.PainlessContextClassInfo;
import org.elasticsearch.painless.action.PainlessContextInfo;
import org.elasticsearch.painless.action.PainlessContextInstanceBindingInfo;
import org.elasticsearch.painless.action.PainlessContextMethodInfo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContextWhitelistProcessor {
    @SuppressForbidden(reason = "retrieving data from an internal API not exposed as part of the REST client")
    public static List<PainlessContextInfo> getContextInfos(String clusterUri) throws IOException {
        URLConnection getContextNames = new URL(
            "http://" + clusterUri + "/_scripts/painless/_context").openConnection();
        XContentParser parser = JsonXContent.jsonXContent.createParser(null, null, getContextNames.getInputStream());
        parser.nextToken();
        parser.nextToken();
        @SuppressWarnings("unchecked")
        List<String> contextNames = (List<String>)(Object)parser.list();
        parser.close();
        ((HttpURLConnection)getContextNames).disconnect();

        List<PainlessContextInfo> contextInfos = new ArrayList<>();

        for (String contextName : contextNames) {
            URLConnection getContextInfo = new URL(
                "http://" + System.getProperty("cluster.uri") + "/_scripts/painless/_context?context=" + contextName).openConnection();
            parser = JsonXContent.jsonXContent.createParser(null, null, getContextInfo.getInputStream());
            contextInfos.add(PainlessContextInfo.fromXContent(parser));
            ((HttpURLConnection)getContextInfo).disconnect();
        }

        contextInfos.sort(Comparator.comparing(PainlessContextInfo::getName));

        return contextInfos;
    }

    public static Map<String, String> getDisplayNames(List<PainlessContextClassInfo> classInfos) {
        Map<String, String> javaNamesToDisplayNames = new HashMap<>();

        for (PainlessContextClassInfo classInfo : classInfos) {
            String className = classInfo.getName();

            if (classInfo.isImported()) {
                javaNamesToDisplayNames.put(className,
                    className.substring(className.lastIndexOf('.') + 1).replace('$', '.'));
            } else {
                javaNamesToDisplayNames.put(className, className.replace('$', '.'));
            }
        }

        return javaNamesToDisplayNames;
    }

    public static String getType(Map<String, String> javaNamesToDisplayNames, String javaType) {
        int arrayDimensions = 0;

        while (javaType.charAt(arrayDimensions) == '[') {
            ++arrayDimensions;
        }

        if (arrayDimensions > 0) {
            if (javaType.charAt(javaType.length() - 1) == ';') {
                javaType = javaType.substring(arrayDimensions + 1, javaType.length() - 1);
            } else {
                javaType = javaType.substring(arrayDimensions);
            }
        }

        if ("Z".equals(javaType) || "boolean".equals(javaType)) {
            javaType = "boolean";
        } else if ("V".equals(javaType) || "void".equals(javaType)) {
            javaType = "void";
        } else if ("B".equals(javaType) || "byte".equals(javaType)) {
            javaType = "byte";
        } else if ("S".equals(javaType) || "short".equals(javaType)) {
            javaType = "short";
        } else if ("C".equals(javaType) || "char".equals(javaType)) {
            javaType = "char";
        } else if ("I".equals(javaType) || "int".equals(javaType)) {
            javaType = "int";
        } else if ("J".equals(javaType) || "long".equals(javaType)) {
            javaType = "long";
        } else if ("F".equals(javaType) || "float".equals(javaType)) {
            javaType = "float";
        } else if ("D".equals(javaType) || "double".equals(javaType)) {
            javaType = "double";
        } else if ("org.elasticsearch.painless.lookup.def".equals(javaType)) {
            javaType = "def";
        } else {
            javaType = javaNamesToDisplayNames.get(javaType);
        }

        while (arrayDimensions-- > 0) {
            javaType += "[]";
        }

        return javaType;
    }

    public static class PainlessMethodInfo implements ToXContentObject {
        private final String declaring;
        private final String name;
        private final String rtn;
        private final List<String> parameters;
        private final Map<String, String> javaNamesToDisplayNames;

        public PainlessMethodInfo(String declaring, String name, String rtn, List<String> parameters,
                                  Map<String, String> javaNamesToDisplayNames) {
            this.declaring = declaring;
            this.name = name;
            this.rtn = rtn;
            this.parameters = parameters;
            this.javaNamesToDisplayNames = javaNamesToDisplayNames;
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(PainlessContextMethodInfo.DECLARING.getPreferredName(), declaring);
            builder.field(PainlessContextMethodInfo.NAME.getPreferredName(), name);
            builder.field(PainlessContextMethodInfo.RTN.getPreferredName(), rtn);
            builder.field(.PainlessContextMethodInfoPARAMETERS.getPreferredName(), parameters);
            builder.endObject();

            return builder;
        }
    }

    public static class PainlessInfos {
        public final Set<PainlessContextClassInfo> classes;
        public final Set<PainlessContextMethodInfo> importedMethods;
        public final Set<PainlessContextClassBindingInfo> classBindings;
        public final Set<PainlessContextInstanceBindingInfo> instanceBindings;

        public final List<PainlessContextClassInfo> common;
        public final Map<String, List<PainlessContextClassInfo>> sorted;


        public PainlessInfos(List<PainlessContextInfo> contexts) {
            classes = getCommon(contexts, PainlessContextInfo::getClasses);
            importedMethods = getCommon(contexts, PainlessContextInfo::getImportedMethods);
            classBindings = getCommon(contexts, PainlessContextInfo::getClassBindings);
            instanceBindings = getCommon(contexts, PainlessContextInfo::getInstanceBindings);

            common = sortClassInfos(classes);
            Map<String, List<PainlessContextClassInfo>> sorted = new HashMap<>();
            for (PainlessContextInfo context : contexts) {
                sorted.put(context.getName(), sortClassInfos(excludeCommonClassInfos(classes, context.getClasses())));
            }
            this.sorted = Collections.unmodifiableMap(sorted);
        }

        private <T> Set<T> getCommon(List<PainlessContextInfo> contexts, Function<PainlessContextInfo,List<T>> getter) {
            Map<T, Integer> infoCounts = new HashMap<>();
            for (PainlessContextInfo contextInfo : contexts) {
                for (T info : getter.apply(contextInfo)) {
                    infoCounts.merge(info, 1, Integer::sum);
                }
            }
            return infoCounts.entrySet().stream().filter(
                e -> e.getValue() == contexts.size()
            ).map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet());
        }

        private List<PainlessContextClassInfo> sortClassInfos(Collection<PainlessContextClassInfo> unsortedClassInfos) {

            List<PainlessContextClassInfo> classInfos = new ArrayList<>(unsortedClassInfos);
            classInfos.removeIf(v ->
                "void".equals(v.getName())  || "boolean".equals(v.getName()) || "byte".equals(v.getName())   ||
                    "short".equals(v.getName()) || "char".equals(v.getName())    || "int".equals(v.getName())    ||
                    "long".equals(v.getName())  || "float".equals(v.getName())   || "double".equals(v.getName()) ||
                    "org.elasticsearch.painless.lookup.def".equals(v.getName())  ||
                    isInternalClass(v.getName())
            );

            classInfos.sort((c1, c2) -> {
                String n1 = c1.getName();
                String n2 = c2.getName();
                boolean i1 = c1.isImported();
                boolean i2 = c2.isImported();

                String p1 = n1.substring(0, n1.lastIndexOf('.'));
                String p2 = n2.substring(0, n2.lastIndexOf('.'));

                int compare = p1.compareTo(p2);

                if (compare == 0) {
                    if (i1 && i2) {
                        compare = n1.substring(n1.lastIndexOf('.') + 1).compareTo(n2.substring(n2.lastIndexOf('.') + 1));
                    } else if (i1 == false && i2 == false) {
                        compare = n1.compareTo(n2);
                    } else {
                        compare = Boolean.compare(i1, i2) * -1;
                    }
                }

                return compare;
            });

            return Collections.unmodifiableList(classInfos);
        }

        private List<PainlessContextClassInfo> excludeCommonClassInfos(
            Set<PainlessContextClassInfo> exclude,
            List<PainlessContextClassInfo> classInfos
        ) {
            classInfos.removeIf(exclude::contains);
            return classInfos;
        }

        private boolean isInternalClass(String javaName) {
            return  javaName.equals("org.elasticsearch.script.ScoreScript") ||
                javaName.equals("org.elasticsearch.xpack.sql.expression.function.scalar.geo.GeoShape") ||
                javaName.equals("org.elasticsearch.xpack.sql.expression.function.scalar.whitelist.InternalSqlScriptUtils") ||
                javaName.equals("org.elasticsearch.xpack.sql.expression.literal.IntervalDayTime") ||
                javaName.equals("org.elasticsearch.xpack.sql.expression.literal.IntervalYearMonth") ||
                javaName.equals("org.elasticsearch.xpack.eql.expression.function.scalar.whitelist.InternalEqlScriptUtils") ||
                javaName.equals("org.elasticsearch.xpack.ql.expression.function.scalar.InternalQlScriptUtils") ||
                javaName.equals("org.elasticsearch.xpack.ql.expression.function.scalar.whitelist.InternalQlScriptUtils") ||
                javaName.equals("org.elasticsearch.script.ScoreScript$ExplanationHolder");
        }
    }
}
