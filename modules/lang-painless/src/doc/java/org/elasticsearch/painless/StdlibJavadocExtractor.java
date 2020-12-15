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

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.painless.action.PainlessContextMethodInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class StdlibJavadocExtractor {
    private final Path root;

    public StdlibJavadocExtractor(String root) {
        this.root = PathUtils.get(root);
    }

    private File openClassFile(String className) {
        int dollarPosition = className.indexOf("$");
        if (dollarPosition >= 0) {
            className = className.substring(0, dollarPosition);
        }
        System.out.println("Stu className: " + className );
        String[] packages = className.split("\\.");
        String path = String.join("/", packages);
        Path classPath = root.resolve(path + ".java");
        return classPath.toFile();
    }

    public ParsedJavaClass parseClass(String className) throws IOException {
        ParsedJavaClass pj = new ParsedJavaClass();
        if (className.contains(".")) { // TODO(stu): handle primitives
            ClassFileVisitor visitor = new ClassFileVisitor();
            CompilationUnit cu = StaticJavaParser.parse(openClassFile(className));
            visitor.visit(cu, pj);
        }
        return pj;
    }

    public static class ParsedJavaClass {
        public final Map<MethodSignature, ParsedMethod> methods;
        public final Map<String, String> fields;

        public ParsedJavaClass() {
            methods = new HashMap<>();
            fields = new HashMap<>();
        }

        public ParsedMethod getMethod(PainlessContextMethodInfo info, Map<String, String> javaNamesToDisplayNames) {
            return methods.get(MethodSignature.fromInfo(info, javaNamesToDisplayNames));
        }

        @Override
        public String toString() {
            return "ParsedJavaClass{" +
                "methods=" + methods +
                '}';
        }

        public void putMethod(MethodDeclaration declaration) {
            methods.put(
                MethodSignature.fromDeclaration(declaration),
                new ParsedMethod(
                    declaration.getJavadoc().toString(),
                    declaration.getParameters()
                        .stream()
                        .map(p -> p.getName().asString())
                        .collect(Collectors.toList())
                )
            );
        }

        public String getField(String name) {
            return fields.get(name);
        }

        public void putField(FieldDeclaration declaration) {
            for (VariableDeclarator var : declaration.getVariables()) {
                fields.put(var.getNameAsString(), declaration.getJavadoc().toString());
            }
        }
    }

    public static class MethodSignature {
        public final String name;
        public final List<String> parameterTypes;

        public MethodSignature(String name, List<String> parameterTypes) {
            this.name = name;
            this.parameterTypes = parameterTypes;
        }

        public static MethodSignature fromInfo(PainlessContextMethodInfo info,Map<String, String> javaNamesToDisplayNames) {
            return new MethodSignature(
                info.getName(),
                // TODO(stu): does this handle everything?
                info.getParameters().stream().map(javaNamesToDisplayNames::get).collect(Collectors.toList())
            );
        }

        public static MethodSignature fromDeclaration(MethodDeclaration declaration) {
            return new MethodSignature(
                declaration.getNameAsString(),
                declaration.getParameters()
                    .stream()
                    .map(p -> p.getType().asString())
                    .collect(Collectors.toList())
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodSignature)) return false;
            MethodSignature that = (MethodSignature) o;
            return Objects.equals(name, that.name) &&
                Objects.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, parameterTypes);
        }
    }

    public static class ParsedMethod {
        public final String javadoc;
        public final List<String> parameterNames;

        public ParsedMethod(String javadoc, List<String> parameterNames) {
            this.javadoc = javadoc;
            this.parameterNames = parameterNames;
        }
    }

    private static class ClassFileVisitor extends VoidVisitorAdapter<ParsedJavaClass> {
        @Override
        public void visit(MethodDeclaration methodDeclaration, ParsedJavaClass parsed) {
            System.out.println("STU visit method: " + methodDeclaration.getName());
            parsed.putMethod(methodDeclaration);
        }

        @Override
        public void visit(FieldDeclaration fieldDeclaration, ParsedJavaClass parsed) {
            System.out.println("STU visit field: " + fieldDeclaration);
            if (fieldDeclaration.hasModifier(Modifier.Keyword.PUBLIC) == false) {
                return;
            }
            parsed.putField(fieldDeclaration);
        }
    }
}
