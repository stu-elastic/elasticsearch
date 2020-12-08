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

package org.elasticsearch.painless;

import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.core.internal.io.IOUtils;
import org.elasticsearch.painless.action.PainlessContextClassBindingInfo;
import org.elasticsearch.painless.action.PainlessContextClassInfo;
import org.elasticsearch.painless.action.PainlessContextConstructorInfo;
import org.elasticsearch.painless.action.PainlessContextFieldInfo;
import org.elasticsearch.painless.action.PainlessContextInfo;
import org.elasticsearch.painless.action.PainlessContextInstanceBindingInfo;
import org.elasticsearch.painless.action.PainlessContextMethodInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.elasticsearch.common.xcontent.ToXContent.EMPTY_PARAMS;
import static org.elasticsearch.painless.action.PainlessContextClassInfo.NAME;
import static org.elasticsearch.painless.action.PainlessContextInfo.INSTANCE_BINDINGS;

public final class ContextApiGenerator {

    public static void main(String[] args) throws IOException {
        // Every context
        List<PainlessContextInfo> contextInfos = getContextInfos();

        // PainlessContextMethodInfo, PainlessContextClassBindingInfo, PainlessContextInstanceBindingInfo
        Set<Object> sharedStaticInfos = createSharedStatics(contextInfos);
        Set<PainlessContextClassInfo> sharedClassInfos = createSharedClasses(contextInfos);


        Path rootDir = resetRootDir();

        Path json = rootDir.resolve("painless-common.json");
        try (PrintStream jsonStream = new PrintStream(
            Files.newOutputStream(json, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE),
            false, StandardCharsets.UTF_8.name())) {

            XContentBuilder builder = XContentFactory.jsonBuilder(jsonStream);
            builder.startObject();
            builder.field(PainlessContextInfo.CLASSES.getPreferredName(),
                          ContextWhitelistProcessor.sortClassInfos(Collections.emptySet(), new ArrayList<>(sharedClassInfos))
            );
            builder.endObject();
            builder.flush();
        }

        Set<PainlessContextInfo> isSpecialized = new HashSet<>();

        for (PainlessContextInfo contextInfo : contextInfos) {
            List<Object> staticInfos = createContextStatics(contextInfo);
            staticInfos = sortStaticInfos(sharedStaticInfos, staticInfos);
            List<PainlessContextClassInfo> classInfos = ContextWhitelistProcessor.sortClassInfos(sharedClassInfos, new ArrayList<>(contextInfo.getClasses()));

            if (staticInfos.isEmpty() == false || classInfos.isEmpty() == false) {
                json = rootDir.resolve(getContextHeader(contextInfo) + ".json");
                try (PrintStream jsonStream = new PrintStream(
                    Files.newOutputStream(json, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE),
                    false, StandardCharsets.UTF_8.name())) {

                    XContentBuilder builder = XContentFactory.jsonBuilder(jsonStream);
                    toXContent(builder, contextInfo, classInfos);
                    builder.flush();
                }
            }
        }
    }

    private static void toXContent(XContentBuilder builder, PainlessContextInfo info, List<PainlessContextClassInfo> classInfos) throws IOException {
        builder.startObject();
        builder.field(PainlessContextInfo.NAME.getPreferredName(), info.getName());
        builder.field(PainlessContextInfo.CLASSES.getPreferredName(), classInfos);
        builder.field(PainlessContextInfo.IMPORTED_METHODS.getPreferredName(), info.getImportedMethods());
        builder.field(PainlessContextInfo.CLASS_BINDINGS.getPreferredName(), info.getClassBindings());
        builder.field(PainlessContextInfo.INSTANCE_BINDINGS.getPreferredName(), info.getInstanceBindings());
        builder.endObject();
    }

    @SuppressForbidden(reason = "retrieving data from an internal API not exposed as part of the REST client")
    private static List<PainlessContextInfo> getContextInfos() throws IOException  {
        URLConnection getContextNames = new URL(
                "http://" + System.getProperty("cluster.uri") + "/_scripts/painless/_context").openConnection();
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

    private static Set<Object> createSharedStatics(List<PainlessContextInfo> contextInfos) {
        Map<Object, Integer> staticInfoCounts = new HashMap<>();

        for (PainlessContextInfo contextInfo : contextInfos) {
            for (PainlessContextMethodInfo methodInfo : contextInfo.getImportedMethods()) {
                staticInfoCounts.merge(methodInfo, 1, Integer::sum);
            }

            for (PainlessContextClassBindingInfo classBindingInfo : contextInfo.getClassBindings()) {
                staticInfoCounts.merge(classBindingInfo, 1, Integer::sum);
            }

            for (PainlessContextInstanceBindingInfo instanceBindingInfo : contextInfo.getInstanceBindings()) {
                staticInfoCounts.merge(instanceBindingInfo, 1, Integer::sum);
            }
        }

        return staticInfoCounts.entrySet().stream().filter(
                e -> e.getValue() == contextInfos.size()
        ).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private static List<Object> createContextStatics(PainlessContextInfo contextInfo) {
        List<Object> staticInfos = new ArrayList<>();

        staticInfos.addAll(contextInfo.getImportedMethods());
        staticInfos.addAll(contextInfo.getClassBindings());
        staticInfos.addAll(contextInfo.getInstanceBindings());

        return staticInfos;
    }

    private static Set<PainlessContextClassInfo> createSharedClasses(List<PainlessContextInfo> contextInfos) {
        Map<PainlessContextClassInfo, Integer> classInfoCounts = new HashMap<>();

        for (PainlessContextInfo contextInfo : contextInfos) {
            for (PainlessContextClassInfo classInfo : contextInfo.getClasses()) {
                classInfoCounts.merge(classInfo, 1, Integer::sum);
            }
        }

        return classInfoCounts.entrySet().stream().filter(
                e -> e.getValue() == contextInfos.size()
        ).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    @SuppressForbidden(reason = "resolve context api directory with environment")
    private static Path resetRootDir() throws IOException {
        Path rootDir = PathUtils.get("./src/main/generated/whitelist-json");
        IOUtils.rm(rootDir);
        Files.createDirectories(rootDir);

        return rootDir;
    }

    private static String getType(Map<String, String> javaNamesToDisplayNames, String javaType) {
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

    private static String getFieldJavaDocLink(PainlessContextFieldInfo fieldInfo) {
        return "{java11-javadoc}/java.base/" + fieldInfo.getDeclaring().replace('.', '/') + ".html#" + fieldInfo.getName();
    }

    private static String getConstructorJavaDocLink(PainlessContextConstructorInfo constructorInfo) {
        StringBuilder javaDocLink = new StringBuilder();

        javaDocLink.append("{java11-javadoc}/java.base/");
        javaDocLink.append(constructorInfo.getDeclaring().replace('.', '/'));
        javaDocLink.append(".html#<init>(");

        for (int parameterIndex = 0;
             parameterIndex < constructorInfo.getParameters().size();
             ++parameterIndex) {

            javaDocLink.append(getLinkType(constructorInfo.getParameters().get(parameterIndex)));

            if (parameterIndex + 1 < constructorInfo.getParameters().size()) {
                javaDocLink.append(",");
            }
        }

        javaDocLink.append(")");

        return javaDocLink.toString();
    }

    private static String getMethodJavaDocLink(PainlessContextMethodInfo methodInfo) {
        StringBuilder javaDocLink = new StringBuilder();

        javaDocLink.append("{java11-javadoc}/java.base/");
        javaDocLink.append(methodInfo.getDeclaring().replace('.', '/'));
        javaDocLink.append(".html#");
        javaDocLink.append(methodInfo.getName());
        javaDocLink.append("(");

        for (int parameterIndex = 0;
             parameterIndex < methodInfo.getParameters().size();
             ++parameterIndex) {

            javaDocLink.append(getLinkType(methodInfo.getParameters().get(parameterIndex)));

            if (parameterIndex + 1 < methodInfo.getParameters().size()) {
                javaDocLink.append(",");
            }
        }

        javaDocLink.append(")");

        return javaDocLink.toString();
    }

    private static String getLinkType(String javaType) {
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
            javaType = "java.lang.Object";
        }

        while (arrayDimensions-- > 0) {
            javaType += "%5B%5D";
        }

        return javaType;
    }

    private static String getContextHeader(PainlessContextInfo contextInfo) {
        return "painless-api-reference-" + contextInfo.getName().replace(" ", "-").replace("_", "-");
    }

    private static String getPackageHeader(String contextHeader, String packageName) {
        return contextHeader + "-" + packageName.replace('.', '-');
    }

    private static String getClassHeader(String contextHeader, String className) {
        return contextHeader + "-" + className.replace('.', '-');
    }

    private static String getContextName(PainlessContextInfo contextInfo) {
        String[] split = contextInfo.getName().split("[_-]");
        StringBuilder contextNameBuilder = new StringBuilder();

        for (String part : split) {
            contextNameBuilder.append(Character.toUpperCase(part.charAt(0)));
            contextNameBuilder.append(part.substring(1));
            contextNameBuilder.append(' ');
        }

        return contextNameBuilder.substring(0, contextNameBuilder.length() - 1);
    }

    private static List<Object> sortStaticInfos(Set<Object> staticExcludes, List<Object> staticInfos) {
        staticInfos = new ArrayList<>(staticInfos);
        staticInfos.removeIf(staticExcludes::contains);

        staticInfos.sort((si1, si2) -> {
            String sv1;
            String sv2;

            if (si1 instanceof PainlessContextMethodInfo) {
                sv1 = ((PainlessContextMethodInfo)si1).getSortValue();
            } else if (si1 instanceof PainlessContextClassBindingInfo) {
                sv1 = ((PainlessContextClassBindingInfo)si1).getSortValue();
            } else if (si1 instanceof PainlessContextInstanceBindingInfo) {
                sv1 = ((PainlessContextInstanceBindingInfo)si1).getSortValue();
            } else {
                throw new IllegalArgumentException("unexpected static info type");
            }

            if (si2 instanceof PainlessContextMethodInfo) {
                sv2 = ((PainlessContextMethodInfo)si2).getSortValue();
            } else if (si2 instanceof PainlessContextClassBindingInfo) {
                sv2 = ((PainlessContextClassBindingInfo)si2).getSortValue();
            } else if (si2 instanceof PainlessContextInstanceBindingInfo) {
                sv2 = ((PainlessContextInstanceBindingInfo)si2).getSortValue();
            } else {
                throw new IllegalArgumentException("unexpected static info type");
            }

            return sv1.compareTo(sv2);
        });

        return staticInfos;
    }

    private static Map<String, String> getDisplayNames(List<PainlessContextClassInfo> classInfos) {
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

    private static boolean isInternalClass(String javaName) {
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

    private ContextApiGenerator() {

    }
}
