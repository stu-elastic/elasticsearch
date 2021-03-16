/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.painless;

import org.elasticsearch.bootstrap.BootstrapInfo;
import org.elasticsearch.painless.antlr.Walker;
import org.elasticsearch.painless.ir.ClassNode;
import org.elasticsearch.painless.lookup.PainlessLookup;
import org.elasticsearch.painless.node.SClass;
import org.elasticsearch.painless.phase.DefaultConstantFoldingOptimizationPhase;
import org.elasticsearch.painless.phase.DefaultIRTreeToASMBytesPhase;
import org.elasticsearch.painless.phase.DefaultStaticConstantExtractionPhase;
import org.elasticsearch.painless.phase.DefaultStringConcatenationOptimizationPhase;
import org.elasticsearch.painless.phase.DocFieldsPhase;
import org.elasticsearch.painless.phase.PainlessIRTreeToASMBytesPhase;
import org.elasticsearch.painless.phase.PainlessSemanticAnalysisPhase;
import org.elasticsearch.painless.phase.PainlessSemanticHeaderPhase;
import org.elasticsearch.painless.phase.PainlessUserTreeToIRTreePhase;
import org.elasticsearch.painless.spi.Whitelist;
import org.elasticsearch.painless.symbol.Decorations.IRNodeDecoration;
import org.elasticsearch.painless.symbol.ScriptScope;
import org.objectweb.asm.util.Printer;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.elasticsearch.painless.WriterConstants.CLASS_NAME;

/**
 * The Compiler is the entry point for generating a Painless script.  The compiler will receive a Painless
 * tree based on the type of input passed in (currently only ANTLR).  Two passes will then be run over the tree,
 * one for analysis and another to generate the actual byte code using ASM using the root of the tree {@link SClass}.
 */
final class Compiler {

    /**
     * Define the class with lowest privileges.
     */
    private static final CodeSource CODESOURCE;

    /**
     * Setup the code privileges.
     */
    static {
        try {
            // Setup the code privileges.
            CODESOURCE = new CodeSource(new URL("file:" + BootstrapInfo.UNTRUSTED_CODEBASE), (Certificate[]) null);
        } catch (MalformedURLException impossible) {
            throw new RuntimeException(impossible);
        }
    }

    /**
     * A secure class loader used to define Painless scripts.
     */
    final class Loader extends SecureClassLoader {
        private final AtomicInteger lambdaCounter = new AtomicInteger(0);

        /**
         * @param parent The parent ClassLoader.
         */
        Loader(ClassLoader parent) {
            super(parent);
        }

        /**
         * Will check to see if the {@link Class} has already been loaded when
         * the {@link PainlessLookup} was initially created.  Allows for {@link Whitelist}ed
         * classes to be loaded from other modules/plugins without a direct relationship
         * to the module's/plugin's {@link ClassLoader}.
         */
        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            Class<?> found = additionalClasses.get(name);
            if (found != null) {
                return found;
            }
            found = painlessLookup.javaClassNameToClass(name);

            return found != null ? found : super.findClass(name);
        }

        /**
         * Generates a Class object from the generated byte code.
         * @param name The name of the class.
         * @param bytes The generated byte code.
         * @return A Class object defining a factory.
         */
        Class<?> defineFactory(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length, CODESOURCE);
        }

        /**
         * Generates a Class object from the generated byte code.
         * @param name The name of the class.
         * @param bytes The generated byte code.
         * @return A Class object extending {@link PainlessScript}.
         */
        Class<? extends PainlessScript> defineScript(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length, CODESOURCE).asSubclass(PainlessScript.class);
        }

        /**
         * Generates a Class object for a lambda method.
         * @param name The name of the class.
         * @param bytes The generated byte code.
         * @return A Class object.
         */
        Class<?> defineLambda(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length, CODESOURCE);
        }

        /**
         * A counter used to generate a unique name for each lambda
         * function/reference class in this classloader.
         */
        int newLambdaIdentifier() {
            return lambdaCounter.getAndIncrement();
        }
    }

    /**
     * Return a new {@link Loader} for a script using the
     * {@link Compiler}'s specified {@link PainlessLookup}.
     */
    public Loader createLoader(ClassLoader parent) {
        return new Loader(parent);
    }

    /**
     * The class/interface the script will implement.
     */
    private final Class<?> scriptClass;

    /**
     * The whitelist the script will use.
     */
    private final PainlessLookup painlessLookup;

    /**
     * Classes that do not exist in the lookup, but are needed by the script factories.
     */
    private final Map<String, Class<?>> additionalClasses;

    /**
     * Standard constructor.
     * @param scriptClass The class/interface the script will implement.
     * @param factoryClass An optional class/interface to create the {@code scriptClass} instance.
     * @param statefulFactoryClass An optional class/interface to create the {@code factoryClass} instance.
     * @param painlessLookup The whitelist the script will use.
     */
    Compiler(Class<?> scriptClass, Class<?> factoryClass, Class<?> statefulFactoryClass, PainlessLookup painlessLookup) {
        this.scriptClass = scriptClass;
        this.painlessLookup = painlessLookup;
        Map<String, Class<?>> additionalClasses = new HashMap<>();
        additionalClasses.put(scriptClass.getName(), scriptClass);
        addFactoryMethod(additionalClasses, factoryClass, "newInstance");
        addFactoryMethod(additionalClasses, statefulFactoryClass, "newFactory");
        addFactoryMethod(additionalClasses, statefulFactoryClass, "newInstance");
        this.additionalClasses = Collections.unmodifiableMap(additionalClasses);
    }

    private static void addFactoryMethod(Map<String, Class<?>> additionalClasses, Class<?> factoryClass, String methodName) {
        if (factoryClass == null) {
            return;
        }

        Method factoryMethod = null;
        for (Method method : factoryClass.getMethods()) {
            if (methodName.equals(method.getName())) {
                factoryMethod = method;
                break;
            }
        }
        if (factoryMethod == null) {
            return;
        }

        additionalClasses.put(factoryClass.getName(), factoryClass);
        for (int i = 0; i < factoryMethod.getParameterTypes().length; ++i) {
            Class<?> parameterClazz = factoryMethod.getParameterTypes()[i];
            additionalClasses.put(parameterClazz.getName(), parameterClazz);
        }
    }

    /**
     * Runs the two-pass compiler to generate a Painless script.
     * @param loader The ClassLoader used to define the script.
     * @param name The name of the script.
     * @param source The source code for the script.
     * @param settings The CompilerSettings to be used during the compilation.
     * @return The ScriptScope used to compile
     */
    ScriptScope compile(Loader loader, String name, String source, CompilerSettings settings) {
        String scriptName = Location.computeSourceName(name);
        ScriptClassInfo scriptClassInfo = new ScriptClassInfo(painlessLookup, scriptClass);
        SClass root = Walker.buildPainlessTree(scriptName, source, settings);
        ScriptScope scriptScope = new ScriptScope(painlessLookup, settings, scriptClassInfo, scriptName, source, root.getIdentifier() + 1);
        new PainlessSemanticHeaderPhase().visitClass(root, scriptScope);
        new PainlessSemanticAnalysisPhase().visitClass(root, scriptScope);
        // TODO: Make this phase optional #60156
        new DocFieldsPhase().visitClass(root, scriptScope);
        new PainlessUserTreeToIRTreePhase().visitClass(root, scriptScope);
        ClassNode classNode = (ClassNode)scriptScope.getDecoration(root, IRNodeDecoration.class).getIRNode();
        new DefaultStringConcatenationOptimizationPhase().visitClass(classNode, null);
        new DefaultConstantFoldingOptimizationPhase().visitClass(classNode, null);
        new DefaultStaticConstantExtractionPhase().visitClass(classNode, scriptScope);
        new PainlessIRTreeToASMBytesPhase().visitScript(classNode);
        byte[] bytes = classNode.getBytes();

        try {
            Class<? extends PainlessScript> clazz = loader.defineScript(CLASS_NAME, bytes);

            for (Map.Entry<String, Object> staticConstant : scriptScope.getStaticConstants().entrySet()) {
                clazz.getField(staticConstant.getKey()).set(null, staticConstant.getValue());
            }

            return scriptScope;
        } catch (Exception exception) {
            // Catch everything to let the user know this is something caused internally.
            throw new IllegalStateException("An internal error occurred attempting to define the script [" + name + "].", exception);
        }
    }

    /**
     * Runs the two-pass compiler to generate a Painless script.  (Used by the debugger.)
     * @param source The source code for the script.
     * @param settings The CompilerSettings to be used during the compilation.
     * @return The bytes for compilation.
     */
    byte[] compile(String name, String source, CompilerSettings settings, Printer debugStream) {
        String scriptName = Location.computeSourceName(name);
        ScriptClassInfo scriptClassInfo = new ScriptClassInfo(painlessLookup, scriptClass);
        SClass root = Walker.buildPainlessTree(scriptName, source, settings);
        ScriptScope scriptScope = new ScriptScope(painlessLookup, settings, scriptClassInfo, scriptName, source, root.getIdentifier() + 1);
        new PainlessSemanticHeaderPhase().visitClass(root, scriptScope);
        new PainlessSemanticAnalysisPhase().visitClass(root, scriptScope);
        // TODO: Make this phase optional #60156
        new DocFieldsPhase().visitClass(root, scriptScope);
        new PainlessUserTreeToIRTreePhase().visitClass(root, scriptScope);
        ClassNode classNode = (ClassNode)scriptScope.getDecoration(root, IRNodeDecoration.class).getIRNode();
        new DefaultStringConcatenationOptimizationPhase().visitClass(classNode, null);
        new DefaultConstantFoldingOptimizationPhase().visitClass(classNode, null);
        new DefaultStaticConstantExtractionPhase().visitClass(classNode, scriptScope);
        classNode.setDebugStream(debugStream);
        new PainlessIRTreeToASMBytesPhase().visitScript(classNode);

        return classNode.getBytes();
    }
}
