/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.compiler.environments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.CompilationUnitIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.interpreter.env.WorseEnv;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.AliasedAPIName;
import com.sun.fortress.nodes.ImportedNames;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Pair;

import edu.rice.cs.plt.collect.Relation;


public class TopLevelEnvGen {

    private static final String STRING_INTERNALNAME = Type.getType(String.class).getInternalName();
    private static final String STRING_DESCRIPTOR = Type.getType(String.class).getDescriptor();

    public static final String API_ENV_SUFFIX = "ApiEnv";
    public static final String COMPONENT_ENV_SUFFIX = "ComponentEnv";


    public static class CompilationUnitResult extends StaticPhaseResult {
        private final Map<APIName, Pair<String,byte[]>> _compUnits;
        public CompilationUnitResult(Map<APIName, Pair<String, byte[]>> compUnits,
                       Iterable<? extends StaticError> errors) {
            super(errors);
            _compUnits = compUnits;
        }
        public Map<APIName, Pair<String, byte[]>> generatedEnvs() { return _compUnits; }
    }

    public static String mangleClassIdentifier(String identifier) {
        String mangledString = identifier.replaceAll("\\.", "\\$");  
        return mangledString;         
    }
    
    /**
     * http://blogs.sun.com/jrose/entry/symbolic_freedom_in_the_vm
     * Dangerous characters are the union of all characters forbidden
     * or otherwise restricted by the JVM specification, plus their mates,
     * if they are brackets.

     * @param identifier
     * @return
     */
    public static String mangleIdentifier(String identifier) {

        // 1. In each accidental escape, replace the backslash with an escape sequence (\-)
        String mangledString = identifier.replaceAll("\\\\", "\\\\-");

        // 2. Replace each dangerous character with an escape sequence (\| for /, etc.)
        mangledString = mangledString.replaceAll("/", "\\\\|");
        mangledString = mangledString.replaceAll("\\.", "\\\\,");
        mangledString = mangledString.replaceAll(";", "\\\\?");
        mangledString = mangledString.replaceAll("\\$", "\\\\%");
        mangledString = mangledString.replaceAll("<", "\\\\^");
        mangledString = mangledString.replaceAll(">", "\\\\_");
        mangledString = mangledString.replaceAll("\\[", "\\\\{");
        mangledString = mangledString.replaceAll("\\]", "\\\\}");
        mangledString = mangledString.replaceAll(":", "\\\\!");

        // Non-standard name-mangling convention.  Michael Spiegel 6/16/2008
        mangledString = mangledString.replaceAll("\\ ", "\\\\~");

        // 3. If the first two steps introduced any change, <em>and</em> if the
        // string does not already begin with a backslash, prepend a null prefix (\=)
        if (!mangledString.equals(identifier) && !(mangledString.charAt(0) == '\\')) {
            mangledString = "\\=" + mangledString;
        }
        return mangledString; 
    }

    /**
     * Given a list of components, generate a Java bytecode compiled environment
     * for each component.
     */
    public static CompilationUnitResult generateApiEnvs(Map<APIName, ApiIndex> apis,
                        GlobalEnvironment env) {

        Map<APIName, Pair<String,byte[]>> compiledApis = new HashMap<APIName, Pair<String,byte[]>>();
        HashSet<StaticError> errors = new HashSet<StaticError>();

        for(APIName apiName : apis.keySet()) {
            String className = NodeUtil.nameString(apiName);
            className = className + API_ENV_SUFFIX;

            try {
                className = mangleClassIdentifier(className);  // Need to mangle the name if it contains "."
                byte[] envClass = generateForCompilationUnit(className, apis.get(apiName), env);
                compiledApis.put(apiName, new Pair<String,byte[]>(className, envClass));
            } catch(StaticError staticError) {
                errors.add(staticError);
            }
        }

        if (errors.isEmpty()) {
            outputClassFiles(compiledApis, API_ENV_SUFFIX, errors);
        }

        return new CompilationUnitResult(compiledApis, errors);
    }
 
    /**
     * Given a list of components, generate a Java bytecode compiled environment
     * for each component.
     */
    public static CompilationUnitResult generateComponentEnvs(Map<APIName, ComponentIndex> components,
                        GlobalEnvironment env) {

        Map<APIName, Pair<String,byte[]>> compiledComponents = new HashMap<APIName, Pair<String,byte[]>>();
        HashSet<StaticError> errors = new HashSet<StaticError>();

        for(APIName componentName : components.keySet()) {
            String className = NodeUtil.nameString(componentName);
            className = className + COMPONENT_ENV_SUFFIX;
            className = mangleClassIdentifier(className);  // Need to mangle the name if it contains "."
            try {
                byte[] envClass = generateForCompilationUnit(className,
                                                             components.get(componentName), env);
                compiledComponents.put(componentName, new Pair<String,byte[]>(className,envClass));
            } catch(StaticError staticError) {
                errors.add(staticError);
            }
        }

        if (errors.isEmpty()) {
            outputClassFiles(compiledComponents, COMPONENT_ENV_SUFFIX, errors);
        }

        return new CompilationUnitResult(compiledComponents, errors);
    }


    /**
     * Given one component, generate a Java bytecode compiled environment
     * for that component.
     */
    private static byte[] generateForCompilationUnit(String className,
                               CompilationUnitIndex compUnitIndex,
                               GlobalEnvironment env) {

        /*
         *  With new ClassWriter(ClassWriter.COMPUTE_FRAMES) everything is
         *  computed automatically. You don't have to call visitFrame, but you
         *  must still call visitMaxs (arguments will be ignored and recomputed).
         *  Using these options is convenient but this has a cost: the COMPUTE_FRAMES option
         *  makes it two times slower.
         *
         *  Currently not a performance bottleneck.
         */
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(Opcodes.V1_5,
            Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL,
            className, null, Type.getType(WorseEnv.class).getInternalName(), null);

        // Implementing "static reflection" for the interpreter
        EnvSymbolNames symbolNames = new EnvSymbolNames();

        writeFields(cw, compUnitIndex, symbolNames);
        writeMethodInit(cw, className);

        writeMethodGetRaw(cw, className, "getApiNull", EnvironmentClass.ENVIRONMENT, symbolNames);
        writeMethodPutRaw(cw, className, "putApi", EnvironmentClass.ENVIRONMENT, symbolNames);
        writeMethodGetRaw(cw, className, "getValueRaw", EnvironmentClass.FVALUE, symbolNames);
        writeMethodPutRaw(cw, className, "putValueRaw", EnvironmentClass.FVALUE, symbolNames);
        writeMethodGetRaw(cw, className, "getTypeNull", EnvironmentClass.FTYPE, symbolNames);
        writeMethodPutRaw(cw, className, "putTypeRaw", EnvironmentClass.FTYPE, symbolNames);
        writeEmptyMethods(cw, className);
        writeRemoveMethods(cw, className);
        writeDumpMethod(cw, className, symbolNames);
        cw.visitEnd();

        return(cw.toByteArray());
    }

    /**
     * Write the import statements as fields in this compiled environment.
     * This method will be invoked by writeFields().
     */
    private static void writeImportFields(ClassWriter cw, CompilationUnitIndex compUnitIndex,
                                          EnvSymbolNames symbolNames) {
        CompilationUnit comp = compUnitIndex.ast();
        final Vector<String> importedApiNames = new Vector<String>();

        for(Import imports : comp.getImports()) {
            if (imports instanceof ImportApi) {
                ImportApi importApi = (ImportApi) imports;
                for (AliasedAPIName api : importApi.getApis()) {
                    importedApiNames.add(NodeUtil.nameString(api.getApi()));                    
                }
            } else if (imports instanceof ImportedNames) {
                ImportedNames importNames = (ImportedNames) imports;
                importedApiNames.add(NodeUtil.nameString(importNames.getApi()));
            } else {
                throw StaticError.make("Unrecognized import type in bytecode generation", imports);
            }
        }
        
        for(String apiName : importedApiNames) {
        	symbolNames.add(EnvironmentClass.ENVIRONMENT, apiName);
            apiName = apiName + EnvironmentClass.ENVIRONMENT.namespace();
            cw.visitField(Opcodes.ACC_PUBLIC, mangleIdentifier(apiName), 
                          EnvironmentClass.ENVIRONMENT.descriptor(), null, null).visitEnd();
        }
    }

    /**
     * Write all the fields that will be used in this compiled environment
     */
    private static void writeFields(ClassWriter cw, CompilationUnitIndex compUnitIndex,
    								EnvSymbolNames symbolNames) {

        // Create all variables as fields in the environment
        for(Id id : compUnitIndex.variables().keySet()) {
            String idString = NodeUtil.nameString(id);
            symbolNames.add(EnvironmentClass.FVALUE, idString);
            idString = idString + EnvironmentClass.FVALUE.namespace();
            cw.visitField(Opcodes.ACC_PUBLIC, mangleIdentifier(idString), EnvironmentClass.FVALUE.descriptor(), null, null).visitEnd();
        }

        // Create all functions as fields in the environment
        for(IdOrOpOrAnonymousName id : compUnitIndex.functions().firstSet()) {
            String idString = NodeUtil.nameString(id);
            symbolNames.add(EnvironmentClass.FVALUE, idString);
            idString = idString + EnvironmentClass.FVALUE.namespace();
            cw.visitField(Opcodes.ACC_PUBLIC, mangleIdentifier(idString), EnvironmentClass.FVALUE.descriptor(), null, null).visitEnd();
        }

        // Create all types as fields in the environment
        for(Id id : compUnitIndex.typeConses().keySet()) {
            String idString = NodeUtil.nameString(id);
            symbolNames.add(EnvironmentClass.FTYPE, idString);
            idString = idString + EnvironmentClass.FTYPE.namespace();
            cw.visitField(Opcodes.ACC_PUBLIC, mangleIdentifier(idString), EnvironmentClass.FTYPE.descriptor(), null, null).visitEnd();
        }
        
        writeImportFields(cw, compUnitIndex, symbolNames);
    }

    /**
     * Generate the default constructor for this class.
     */
    private static void writeMethodInit(ClassWriter cw, String className) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                Type.getType(WorseEnv.class).getInternalName(), "<init>",
                "()V");
        mv.visitInsn(Opcodes.RETURN);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "L" + className + ";", null, l0, l1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * Implementing "static reflection" for the method getFooRaw so the
     * interpreter has O(log n) lookups based on the hash values of String names
     * in this namespace.
     */
    private static void writeMethodGetRaw(ClassWriter cw, String className,
            String methodName, EnvironmentClass environmentClass, EnvSymbolNames symbolNames) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
            methodName,
            "(Ljava/lang/String;)" +
            environmentClass.descriptor(),
            null, null);
        mv.visitCode();

        Label defQueryHashCode = new Label();
        mv.visitLabel(defQueryHashCode);

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I");
        mv.visitVarInsn(Opcodes.ISTORE, 2);
        Label beginLoop = new Label();
        mv.visitLabel(beginLoop);

        Relation<String, Integer> hashCodeRelation = symbolNames.makeHashCodeRelation(environmentClass);
        ArrayList<Integer> sortedCodes = new ArrayList<Integer>(hashCodeRelation.secondSet());
        Collections.sort(sortedCodes);
        getRawHelper(mv, className, hashCodeRelation, environmentClass, sortedCodes);

        Label endFunction = new Label();
        mv.visitLabel(endFunction);
        mv.visitLocalVariable("this", "L" + className + ";", null, defQueryHashCode, endFunction, 0);
        mv.visitLocalVariable("queryString", "Ljava/lang/String;", null, defQueryHashCode, endFunction, 1);
        mv.visitLocalVariable("queryHashCode", "I", null, beginLoop, endFunction, 2);
        mv.visitMaxs(2, 3);
        mv.visitEnd();
    }

    private static void getRawHelper(MethodVisitor mv, String className,
            Relation<String, Integer> hashCodeRelation, EnvironmentClass environmentClass,
            List<Integer> sortedCodes) {
        if (sortedCodes.size() < 9) {
            getRawBaseCase(mv, className, hashCodeRelation, environmentClass, sortedCodes);
        } else {
            Integer middleCode = sortedCodes.get(sortedCodes.size() / 2);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitLdcInsn(middleCode);
            Label startRightHalf = new Label();
            mv.visitJumpInsn(Opcodes.IF_ICMPGE, startRightHalf);
            List<Integer> leftCodes = sortedCodes.subList(0, sortedCodes.size() / 2);
            List<Integer> rightCodes = sortedCodes.subList(sortedCodes.size() / 2, sortedCodes.size());
            getRawHelper(mv, className, hashCodeRelation, environmentClass, leftCodes);
            mv.visitLabel(startRightHalf);
            getRawHelper(mv, className, hashCodeRelation, environmentClass, rightCodes);
        }
    }

    private static void getRawBaseCase(MethodVisitor mv, String className,
            Relation<String, Integer> hashCodeRelation, EnvironmentClass environmentClass,
            List<Integer> sortedCodes) {
        for(Integer testHashCode : sortedCodes) {
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitLdcInsn(testHashCode);
            Label beforeInnerLoop = new Label();
            Label afterInnerLoop = new Label();
            mv.visitJumpInsn(Opcodes.IF_ICMPNE, afterInnerLoop);
            mv.visitLabel(beforeInnerLoop);

            for(String testString : hashCodeRelation.matchSecond(testHashCode)) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitLdcInsn(testString);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
                Label beforeReturn = new Label();
                Label afterReturn = new Label();
                mv.visitJumpInsn(Opcodes.IFEQ, afterReturn);
                mv.visitLabel(beforeReturn);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                String idString = testString + environmentClass.namespace();
                mv.visitFieldInsn(Opcodes.GETFIELD, className,
                    mangleIdentifier(idString),
                        environmentClass.descriptor());
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitLabel(afterReturn);
            }

            mv.visitLabel(afterInnerLoop);
        }

        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
    }


    /**
     * Implementing "static reflection" for the method putFooRaw so the
     * interpreter has O(log n) lookups based on the hash values of String names
     * in this namespace.
     */
    private static void writeMethodPutRaw(ClassWriter cw, String className,
            String methodName, EnvironmentClass environmentClass, EnvSymbolNames symbolNames) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
            methodName,
            "(" + STRING_DESCRIPTOR + environmentClass.descriptor() + ")V",
            null, null);
        mv.visitCode();


        Label defQueryHashCode = new Label();
        mv.visitLabel(defQueryHashCode);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STRING_INTERNALNAME, "hashCode", "()I");
        mv.visitVarInsn(Opcodes.ISTORE, 3);
        Label beginLoop = new Label();
        mv.visitLabel(beginLoop);

        Relation<String, Integer> hashCodeRelation = symbolNames.makeHashCodeRelation(environmentClass);        
        ArrayList<Integer> sortedCodes = new ArrayList<Integer>(hashCodeRelation.secondSet());
        Collections.sort(sortedCodes);
        putRawHelper(mv, className, environmentClass, hashCodeRelation, sortedCodes);

        Label endFunction = new Label();
        mv.visitLabel(endFunction);
        mv.visitLocalVariable("this", "L" + className + ";", null,
                defQueryHashCode, endFunction, 0);
        mv.visitLocalVariable("queryString", STRING_DESCRIPTOR, null,
                defQueryHashCode, endFunction, 1);
        mv.visitLocalVariable("value", environmentClass.descriptor(), null, defQueryHashCode, endFunction, 2);
        mv.visitLocalVariable("queryHashCode", "I", null, beginLoop, endFunction, 3);
        mv.visitMaxs(2, 4);
        mv.visitEnd();
    }

    private static void putRawHelper(MethodVisitor mv, String className,
            EnvironmentClass environmentClass, Relation<String, Integer> hashCodeRelation,
            List<Integer> sortedCodes) {
        if (sortedCodes.size() < 9) {
            putRawBaseCase(mv, className, environmentClass, hashCodeRelation, sortedCodes);
        } else {
            Integer middleCode = sortedCodes.get(sortedCodes.size() / 2);
            mv.visitVarInsn(Opcodes.ILOAD, 3);
            mv.visitLdcInsn(middleCode);
            Label startRightHalf = new Label();
            mv.visitJumpInsn(Opcodes.IF_ICMPGE, startRightHalf);
            List<Integer> leftCodes = sortedCodes.subList(0, sortedCodes.size() / 2);
            List<Integer> rightCodes = sortedCodes.subList(sortedCodes.size() / 2, sortedCodes.size());
            putRawHelper(mv, className, environmentClass, hashCodeRelation, leftCodes);
            mv.visitLabel(startRightHalf);
            putRawHelper(mv, className, environmentClass, hashCodeRelation, rightCodes);
        }
    }

    private static void putRawBaseCase(MethodVisitor mv, String className,
            EnvironmentClass environmentClass, Relation<String, Integer> hashCodeRelation,
            List<Integer> sortedCodes) {
        for(Integer testHashCode : sortedCodes) {
            mv.visitVarInsn(Opcodes.ILOAD, 3);
            mv.visitLdcInsn(testHashCode);
            Label beforeInnerLoop = new Label();
            Label afterInnerLoop = new Label();
            mv.visitJumpInsn(Opcodes.IF_ICMPNE, afterInnerLoop);
            mv.visitLabel(beforeInnerLoop);

            for(String testString : hashCodeRelation.matchSecond(testHashCode)) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitLdcInsn(testString);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
                Label beforeSetValue = new Label();
                Label afterSetValue = new Label();
                mv.visitJumpInsn(Opcodes.IFEQ, afterSetValue);
                mv.visitLabel(beforeSetValue);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                String idString = testString + environmentClass.namespace();
                mv.visitFieldInsn(Opcodes.PUTFIELD, className,
                        mangleIdentifier(idString), environmentClass.descriptor());
                mv.visitInsn(Opcodes.RETURN);
                mv.visitLabel(afterSetValue);
            }
            mv.visitLabel(afterInnerLoop);
        }
        mv.visitInsn(Opcodes.RETURN);
    }

    private static void writeNullGetter(ClassWriter cw, String className, String methodName, String signature) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, signature, null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "L" + className + ";", null, l0, l1, 0);
        mv.visitLocalVariable("str", "Ljava/lang/String;", null, l0, l1, 1);
        mv.visitMaxs(1, 2);
        mv.visitEnd();
    }

    private static void writeNullSetter(ClassWriter cw, String className, String methodName, String signature) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, signature, null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitInsn(Opcodes.RETURN);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "L" + className + ";", null, l0, l1, 0);
        mv.visitLocalVariable("str", "Ljava/lang/String;", null, l0, l1, 1);
        mv.visitLocalVariable("f2", "Ljava/lang/Number;", null, l0, l1, 2);
        mv.visitMaxs(0, 3);
        mv.visitEnd();
    }

    private static void writeRemoveMethod(ClassWriter cw, String className, String methodName,
                                          String invokeMethod, EnvironmentClass environmentClass) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, invokeMethod,
                           "(Ljava/lang/String;" + environmentClass.descriptor() + ")V");
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitInsn(Opcodes.RETURN);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", "L" + className + ";", null, l0, l2, 0);
        mv.visitLocalVariable("name", "Ljava/lang/String;", null, l0, l2, 1);
        mv.visitMaxs(3, 2);
        mv.visitEnd();
    }

    private static void writeEmptyMethods(ClassWriter cw, String className) {

        writeNullGetter(cw, className, "getBoolNull", "(Ljava/lang/String;)Ljava/lang/Boolean;");
        writeNullGetter(cw, className, "getIntNull", "(Ljava/lang/String;)Ljava/lang/Number;");
        writeNullGetter(cw, className, "getNatNull", "(Ljava/lang/String;)Ljava/lang/Number;");

        writeNullSetter(cw, className, "putBoolRaw", "(Ljava/lang/String;Ljava/lang/Boolean;)V");
        writeNullSetter(cw, className, "putIntRaw", "(Ljava/lang/String;Ljava/lang/Number;)V");
        writeNullSetter(cw, className, "putNatRaw", "(Ljava/lang/String;Ljava/lang/Number;)V");
    }

    private static void writeRemoveMethods(ClassWriter cw, String className) {
        writeRemoveMethod(cw, className, "removeVar", "putValueRaw", EnvironmentClass.FVALUE);
        writeRemoveMethod(cw, className, "removeType", "putTypeRaw", EnvironmentClass.FTYPE);
    }

    private static void writeDumpMethod(ClassWriter cw, String className,
                                        EnvSymbolNames symbolNames) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                               "dump", "(Ljava/lang/Appendable;)Ljava/lang/Appendable;",
                               null, new String[] { "java/io/IOException" });
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, className, "within", Type.getType(HasAt.class).getDescriptor());
        Label l1 = new Label();
        mv.visitJumpInsn(Opcodes.IFNULL, l1);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, className, "within", Type.getType(HasAt.class).getDescriptor());
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getType(HasAt.class).getInternalName(), "at", "()Ljava/lang/String;");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/Appendable", "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
        mv.visitInsn(Opcodes.POP);
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("\n");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/Appendable", "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
        mv.visitInsn(Opcodes.POP);
        Label l4 = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, l4);
        mv.visitLabel(l1);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("Not within anything.\n");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/Appendable", "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
        mv.visitInsn(Opcodes.POP);
        mv.visitLabel(l4);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, className, "verboseDump", "Z");
        Label l5 = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, l5);
        int linebreaks = dumpFields(mv, className, EnvironmentClass.FVALUE, symbolNames, 0);
        dumpFields(mv, className, EnvironmentClass.FTYPE, symbolNames, linebreaks);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("\n");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/Appendable", "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
        mv.visitInsn(Opcodes.POP);
        mv.visitLabel(l5);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ARETURN);
        Label l9 = new Label();
        mv.visitLabel(l9);
        mv.visitLocalVariable("this", "L" + className + ";", null, l0, l9, 0);
        mv.visitLocalVariable("a", "Ljava/lang/Appendable;", null, l0, l9, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private static int dumpFields(MethodVisitor mv, String className,
                                  EnvironmentClass eClass,
                                  EnvSymbolNames symbolNames,
                                  int linebreaks) {
        for (String fieldName : symbolNames.getSymbolNames(eClass)) {
            Label l6 = new Label();
            mv.visitLabel(l6);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn("(" + fieldName + " = ");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/Appendable",
                    "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
            mv.visitInsn(Opcodes.POP);
            Label l7 = new Label();
            mv.visitLabel(l7);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            String idString = fieldName + eClass.namespace();
            mv.visitFieldInsn(Opcodes.GETFIELD, className,
                    mangleIdentifier(idString), eClass.descriptor());
            Label l8 = new Label();
            mv.visitJumpInsn(Opcodes.IFNULL, l8);
            Label l9 = new Label();
            mv.visitLabel(l9);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, className,
                    mangleIdentifier(idString), eClass.descriptor());
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, eClass
                    .internalName(), "toString", "()Ljava/lang/String;");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/Appendable",
                    "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
            mv.visitInsn(Opcodes.POP);
            Label afterNull = new Label();
            mv.visitJumpInsn(Opcodes.GOTO, afterNull);
            mv.visitLabel(l8);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn("null");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/Appendable",
                    "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
            mv.visitInsn(Opcodes.POP);
            mv.visitLabel(afterNull);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn(") ");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/Appendable",
                    "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
            mv.visitInsn(Opcodes.POP);
            linebreaks = (linebreaks + 1) % 5;
            if (linebreaks == 0) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitLdcInsn("\n");
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                        "java/lang/Appendable", "append",
                        "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
                mv.visitInsn(Opcodes.POP);
            }
        }

        return linebreaks;
    }

    private static void outputClassFiles(Map<APIName, Pair<String,byte[]>> compiledCompUnits,
                            String classSuffix, HashSet<StaticError> errors) {
        for (APIName componentName : compiledCompUnits.keySet()) {
            Pair<String,byte[]> compOutput = compiledCompUnits.get(componentName);
            String fileName = ProjectProperties.BYTECODE_CACHE_DIR + File.separator + compOutput.getA() + ".class";
             outputClassFile(compOutput.getB(), fileName, errors); 
        }
    }

    /**
     * Given a Java bytecode class stored in a byte array, save that
     * class into a file on disk.
     * @throws IOException
     */
    private static void outputClassFile(byte[] bytecode, String fileName,
                                           HashSet<StaticError> errors) {
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(new File(fileName));
            outStream.write(bytecode);
        } catch (IOException e) {
            errors.add(new WrappedException(e));
        } finally {
            try {
            	if (outStream != null)
            		outStream.close();
            } catch(IOException e) {
                errors.add(new WrappedException(e));
            }
        }
    }

}


