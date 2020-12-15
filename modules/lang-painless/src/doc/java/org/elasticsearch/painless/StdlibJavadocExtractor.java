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
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.elasticsearch.common.io.PathUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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

    public ParsedJavaClass getJavadoc(String className) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(openClassFile(className));
        ClassFileVisitor visitor = new ClassFileVisitor();
        ParsedJavaClass pj = new ParsedJavaClass();
        visitor.visit(cu, pj);
        return pj;
    }

    public static class ParsedJavaClass {
        private final Map<String, String> methods;

        public ParsedJavaClass() {
            methods = new HashMap<>();
        }

        public String getMethod(String name) {
            return methods.get(name);
        }

        public void putMethod(String name, String javadoc) {
            methods.put(name, javadoc);
        }
    }

    private static class ClassFileVisitor extends VoidVisitorAdapter<ParsedJavaClass> {
        public Map<String, String> methods;

        public ClassFileVisitor() {
            this.methods = new HashMap<>();
        }

        @Override
        public void visit(MethodDeclaration md, ParsedJavaClass parsed) {
            System.out.println("STU visit method: " + md.getName() );
            parsed.putMethod(md.getNameAsString(), md.getJavadoc().toString());
        }
    }
}
