/*******************************************************************************
    Copyright now,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.environments;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.compiler.index.*;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.Pair;
import edu.rice.cs.plt.collect.PredicateSet;
import edu.rice.cs.plt.collect.Relation;
import org.objectweb.asm.*;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


public class TopLevelEnvGen {

    private static final String STRING_INTERNALNAME = Type.getType(String.class).getInternalName();
    private static final String STRING_DESCRIPTOR = Type.getType(String.class).getDescriptor();

    public static final String API_ENV_SUFFIX = "ApiEnv";
    public static final String COMPONENT_ENV_SUFFIX = "ComponentEnv";


    public static class CompilationUnitResult extends StaticPhaseResult {
        private final Map<APIName, Pair<String, byte[]>> _compUnits;

        public CompilationUnitResult(Map<APIName, Pair<String, byte[]>> compUnits, Iterable<? extends StaticError> errors) {
            super(errors);
            _compUnits = compUnits;
        }

        public Map<APIName, Pair<String, byte[]>> generatedEnvs() {
            return _compUnits;
        }
    }

    /**
     * Given a list of components, generate a Java bytecode compiled environment
     * for each component.
     */
    public static CompilationUnitResult generateApiEnvs(Map<APIName, ApiIndex> apis) {

        Map<APIName, Pair<String, byte[]>> compiledApis = new HashMap<APIName, Pair<String, byte[]>>();
        HashSet<StaticError> errors = new HashSet<StaticError>();

        for (Map.Entry<APIName, ApiIndex> api : apis.entrySet()) {
            String className = NamingCzar.classNameForApiEnvironment(api.getKey());

            try {
                byte[] envClass = generateForCompilationUnit(className, api.getValue());
                compiledApis.put(api.getKey(), new Pair<String, byte[]>(className, envClass));
            }
            catch (StaticError staticError) {
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
    public static CompilationUnitResult generateComponentEnvs(Map<APIName, ComponentIndex> components) {

        Map<APIName, Pair<String, byte[]>> compiledComponents = new HashMap<APIName, Pair<String, byte[]>>();
        HashSet<StaticError> errors = new HashSet<StaticError>();

        for (Map.Entry<APIName, ComponentIndex> component : components.entrySet()) {
            String className = NamingCzar.classNameForComponentEnvironment(component.getKey());
            try {
                byte[] envClass = generateForCompilationUnit(className, component.getValue());
                compiledComponents.put(component.getKey(), new Pair<String, byte[]>(className, envClass));
            }
            catch (StaticError staticError) {
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
    private static byte[] generateForCompilationUnit(String className, CompilationUnitIndex compUnitIndex) {

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

        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER + Opcodes.ACC_FINAL, className, null, Type.getType(BaseEnv.class).getInternalName(), null);

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

        return (cw.toByteArray());
    }

    /**
     * Write the import statements as fields in this compiled environment.
     * This method will be invoked by writeFields().
     */
    private static void writeImportFields(ClassWriter cw, CompilationUnitIndex compUnitIndex, EnvSymbolNames symbolNames) {
        CompilationUnit comp = compUnitIndex.ast();
        final Set<String> importedApiNames = new HashSet<String>();

        for (Import imports : comp.getImports()) {
            if (imports instanceof ImportApi) {
                ImportApi importApi = (ImportApi) imports;
                for (AliasedAPIName api : importApi.getApis()) {
                    importedApiNames.add(NodeUtil.nameString(api.getApiName()));
                }
            } else if (imports instanceof ImportedNames) {
                ImportedNames importNames = (ImportedNames) imports;
                importedApiNames.add(NodeUtil.nameString(importNames.getApiName()));
            } else {
                throw StaticError.make("Unrecognized import type in bytecode generation", imports);
            }
        }

        // XXX: SUPER DUPER HACKY
        // Rewrite the AST to include these builtin imports!
        for (String builtinLib : WellKnownNames.defaultLibrary()) {
            importedApiNames.add(builtinLib);
        }

        // Any names that are exported, are also "imported", which is to say,
        // the disambiguator will generate references to them, so we had better
        // have an answer.
        if (comp instanceof Component) {
            Component ccomp = (Component) comp;
            for (APIName api : ccomp.getExports()) {
                importedApiNames.add(NodeUtil.nameString(api));
            }
        }

        namesToFields(EnvironmentClass.ENVIRONMENT, cw, symbolNames, importedApiNames);
    }

    /**
     * Write all the fields that will be used in this compiled environment
     */
    private static void writeFields(ClassWriter cw, CompilationUnitIndex compUnitIndex, EnvSymbolNames symbolNames) {

        // Create all variables as fields in the environment
        Set<String> idStringSet = new HashSet<String>();
        for (Id id : compUnitIndex.variables().keySet()) {
            String idString = NodeUtil.nameString(id);
            if (idString.equals("_")) {
                Variable v = compUnitIndex.variables().get(id);
                if (v instanceof DeclaredVariable) {
                    DeclaredVariable dv = (DeclaredVariable) v;
                    LValue lvb = dv.ast();
                    idString = WellKnownNames.tempForUnderscore(lvb.getName());
                } else {
                    throw new Error("unhandled case for _ variable");
                }
                // System.err.println(v);
                // apply further mangling.
            }
            idStringSet.add(idString);
        }
        namesToFields(EnvironmentClass.FVALUE, cw, symbolNames, idStringSet);

        // Create all functions as fields in the environment
        idStringSet.clear();
        for (IdOrOpOrAnonymousName id : compUnitIndex.functions().firstSet()) {
            String idString = NodeUtil.nameString(id);
            idStringSet.add(idString);
        }
        namesToFields(EnvironmentClass.FVALUE, cw, symbolNames, idStringSet);

        // Create all types as fields in the environment
        idStringSet.clear();
        for (Id id : compUnitIndex.typeConses().keySet()) {
            String idString = NodeUtil.nameString(id);
            idStringSet.add(idString);
        }
        namesToFields(EnvironmentClass.FTYPE, cw, symbolNames, idStringSet);

        // Special case for singleton objects; get to the object through
        // its type
        idStringSet.clear();
        for (Id id : compUnitIndex.typeConses().keySet()) {
            TypeConsIndex tci = compUnitIndex.typeConses().get(id);
            if (tci instanceof ObjectTraitIndex) {
                ObjectTraitIndex oti = (ObjectTraitIndex) tci;
                if (oti.constructor().isNone()) {
                    String idString = WellKnownNames.obfuscatedSingletonConstructorName(NodeUtil.nameString(id), id);
                    idStringSet.add(idString);
                }
            }
        }
        namesToFields(EnvironmentClass.FVALUE, cw, symbolNames, idStringSet);

        handleWeirdToplevelDecls(cw, compUnitIndex, symbolNames);

        writeImportFields(cw, compUnitIndex, symbolNames);
    }

    /**
     * Emits additional names for overloaded functions,
     * for temporaries generated by multiple assignment/init,
     * and for object expressions.
     *
     * @param cw
     * @param compUnitIndex
     * @param symbolNames
     */
    private static void handleWeirdToplevelDecls(final ClassWriter cw, CompilationUnitIndex compUnitIndex, final EnvSymbolNames symbolNames) {
        CompilationUnit compUnit = compUnitIndex.ast();
        if (compUnit instanceof Component) {
            Component component = (Component) compUnit;
            List<Decl> decls = component.getDecls();
            for (Decl decl : decls) {
                if (decl instanceof _RewriteFnOverloadDecl) {
                    _RewriteFnOverloadDecl rewrite = (_RewriteFnOverloadDecl) decl;
                    String idString = NodeUtil.nameString(rewrite.getName());
                    nameToField(EnvironmentClass.FVALUE, cw, symbolNames, idString);
                } else if (decl instanceof VarDecl) {
                    // The interpreter rewrites a temporary for multiple assignment.
                    VarDecl vd = (VarDecl) decl;
                    List<LValue> lhs = vd.getLhs();
                    if (lhs.size() > 1) {
                        String idString = WellKnownNames.tempTupleName(vd);
                        nameToField(EnvironmentClass.FVALUE, cw, symbolNames, idString);
                    }
                }
            }

            // Scan for all object exprs in a component; the interpreter will
            // promote those to top level.
            NodeDepthFirstVisitor_void visitor = new NodeDepthFirstVisitor_void() {

                @Override
                public void forObjectExprOnly(ObjectExpr that) {
                    String idString = WellKnownNames.objectExprName(that);
                    nameToField(EnvironmentClass.FVALUE, cw, symbolNames, idString);
                    nameToField(EnvironmentClass.FTYPE, cw, symbolNames, idString);
                    // super.forObjectExprOnly(that);
                }

            };
            compUnit.accept(visitor);
        }
    }

    private static void namesToFields(EnvironmentClass nameSpace, ClassWriter cw, EnvSymbolNames symbolNames, Set<String> idStringSet) {
        for (String idString : idStringSet) {
            nameToField(nameSpace, cw, symbolNames, idString);
        }
    }

    private static void nameToField(EnvironmentClass nameSpace, ClassWriter cw, EnvSymbolNames symbolNames, String idString) {
        symbolNames.add(nameSpace, idString);
        idString = idString + nameSpace.namespace();
        cw.visitField(Opcodes.ACC_PUBLIC, Naming.mangleIdentifier(idString), nameSpace.descriptor(), null, null).visitEnd();
        return;
    }

    /**
     * Generate the default constructor for this class.
     * This constructors calls the method setToplevel().
     * If this environment is not a top-level environment, then a default
     * constructor does not need to be created.  (ASM will generate a
     * default constructor).
     */
    private static void writeMethodInit(ClassWriter cw, String className) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getType(BaseEnv.class).getInternalName(), "<init>", "()V");
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "setTopLevel", "()V");
        mv.visitInsn(Opcodes.RETURN);
        // See comment above on ClassWriter.COMPUTE_FRAMES
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * Implementing "static reflection" for the method getFooRaw so the
     * interpreter uses a switch instruction for ***GetRaw
     * based on the hash values of String names in this namespace.
     */
    private static void writeMethodGetRaw(ClassWriter cw, String className, String methodName, EnvironmentClass environmentClass, EnvSymbolNames symbolNames) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, "(Ljava/lang/String;)" + environmentClass.descriptor(), null, null);
        mv.visitCode();

        Label beginFunction = new Label();
        mv.visitLabel(beginFunction);

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I");
        mv.visitVarInsn(Opcodes.ISTORE, 2);
        Label beginLoop = new Label();
        mv.visitLabel(beginLoop);

        Relation<String, Integer> hashCodeRelation = symbolNames.makeHashCodeRelation(environmentClass);
        ArrayList<Integer> sortedCodes = new ArrayList<Integer>(hashCodeRelation.secondSet());
        Collections.sort(sortedCodes);
        Label returnNull = new Label();

        getRawHelper(mv, className, hashCodeRelation, environmentClass, sortedCodes, returnNull);

        mv.visitLabel(returnNull);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);

        Label endFunction = new Label();
        mv.visitLabel(endFunction);
        mv.visitLocalVariable("this",Naming.internalToDesc(className), null, beginFunction, endFunction, 0);
        mv.visitLocalVariable("queryString", "Ljava/lang/String;", null, beginFunction, endFunction, 1);
        mv.visitLocalVariable("queryHashCode", "I", null, beginLoop, endFunction, 2);
        // See comment above on ClassWriter.COMPUTE_FRAMES
        mv.visitMaxs(2, 3);
        mv.visitEnd();
    }

    private static void getRawHelper(MethodVisitor mv, String className, Relation<String, Integer> hashCodeRelation, EnvironmentClass environmentClass, List<Integer> sortedCodes, Label returnNull) {
        int[] codes = new int[sortedCodes.size()];
        Label[] labels = new Label[sortedCodes.size()];
        for (int i = 0; i < codes.length; i++) {
            codes[i] = sortedCodes.get(i);
            Label label = new Label();
            labels[i] = label;
        }
        mv.visitVarInsn(Opcodes.ILOAD, 2);
        mv.visitLookupSwitchInsn(returnNull, codes, labels);
        for (int i = 0; i < codes.length; i++) {
            mv.visitLabel(labels[i]);
            getRawBaseCase(mv, className, hashCodeRelation, environmentClass, codes[i], returnNull);
        }
    }

    private static void getRawBaseCase(MethodVisitor mv, String className, Relation<String, Integer> hashCodeRelation, EnvironmentClass environmentClass, int code, Label returnNull) {

        PredicateSet<String> strings = hashCodeRelation.matchSecond(code);
        for (String testString : strings) {
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn(testString);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
            Label afterReturn = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, afterReturn);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            String idString = testString + environmentClass.namespace();
            mv.visitFieldInsn(Opcodes.GETFIELD, className, Naming.mangleIdentifier(idString), environmentClass.descriptor());
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(afterReturn);
        }
        mv.visitJumpInsn(Opcodes.GOTO, returnNull);
    }


    /**
     * Implementing "static reflection" for the method putRaw so the
     * interpreter uses a switch instruction for ***PutRaw
     * based on the hash values of String names in this namespace.
     */
    private static void writeMethodPutRaw(ClassWriter cw, String className, String methodName, EnvironmentClass environmentClass, EnvSymbolNames symbolNames) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, "(" + STRING_DESCRIPTOR + environmentClass.descriptor() + ")V", null, null);
        mv.visitCode();

        Label beginFunction = new Label();
        mv.visitLabel(beginFunction);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STRING_INTERNALNAME, "hashCode", "()I");
        mv.visitVarInsn(Opcodes.ISTORE, 3);
        Label beginLoop = new Label();
        mv.visitLabel(beginLoop);

        Relation<String, Integer> hashCodeRelation = symbolNames.makeHashCodeRelation(environmentClass);
        ArrayList<Integer> sortedCodes = new ArrayList<Integer>(hashCodeRelation.secondSet());
        Collections.sort(sortedCodes);
        Label notFound = new Label();
        putRawHelper(mv, className, environmentClass, hashCodeRelation, sortedCodes, notFound);
        mv.visitLabel(notFound);
        mv.visitInsn(Opcodes.RETURN);
        Label endFunction = new Label();
        mv.visitLabel(endFunction);
        mv.visitLocalVariable("this",Naming.internalToDesc(className), null, beginFunction, endFunction, 0);
        mv.visitLocalVariable("queryString", STRING_DESCRIPTOR, null, beginFunction, endFunction, 1);
        mv.visitLocalVariable("value", environmentClass.descriptor(), null, beginFunction, endFunction, 2);
        mv.visitLocalVariable("queryHashCode", "I", null, beginLoop, endFunction, 3);
        // See comment above on ClassWriter.COMPUTE_FRAMES
        mv.visitMaxs(2, 4);
        mv.visitEnd();
    }

    private static void putRawHelper(MethodVisitor mv, String className, EnvironmentClass environmentClass, Relation<String, Integer> hashCodeRelation, List<Integer> sortedCodes, Label notFound) {

        int[] codes = new int[sortedCodes.size()];
        Label[] labels = new Label[sortedCodes.size()];
        for (int i = 0; i < codes.length; i++) {
            codes[i] = sortedCodes.get(i);
            Label label = new Label();
            labels[i] = label;
        }
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitLookupSwitchInsn(notFound, codes, labels);
        for (int i = 0; i < codes.length; i++) {
            mv.visitLabel(labels[i]);
            putRawBaseCase(mv, className, hashCodeRelation, environmentClass, codes[i], notFound);
        }

    }

    private static void putRawBaseCase(MethodVisitor mv, String className, Relation<String, Integer> hashCodeRelation, EnvironmentClass environmentClass, int code, Label notFound) {

        PredicateSet<String> strings = hashCodeRelation.matchSecond(code);
        for (String testString : strings) {
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn(testString);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
            Label afterSetValue = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, afterSetValue);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            String idString = testString + environmentClass.namespace();
            mv.visitFieldInsn(Opcodes.PUTFIELD, className, Naming.mangleIdentifier(idString), environmentClass.descriptor());
            mv.visitInsn(Opcodes.RETURN);
            mv.visitLabel(afterSetValue);
        }
        mv.visitJumpInsn(Opcodes.GOTO, notFound);
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
        mv.visitLocalVariable("this",Naming.internalToDesc(className), null, l0, l1, 0);
        mv.visitLocalVariable("str", "Ljava/lang/String;", null, l0, l1, 1);
        // See comment above on ClassWriter.COMPUTE_FRAMES
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
        mv.visitLocalVariable("this",Naming.internalToDesc(className), null, l0, l1, 0);
        mv.visitLocalVariable("str", "Ljava/lang/String;", null, l0, l1, 1);
        mv.visitLocalVariable("f2", "Ljava/lang/Number;", null, l0, l1, 2);
        // See comment above on ClassWriter.COMPUTE_FRAMES
        mv.visitMaxs(0, 3);
        mv.visitEnd();
    }

    private static void writeRemoveMethod(ClassWriter cw, String className, String methodName, String invokeMethod, EnvironmentClass environmentClass) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, invokeMethod, "(Ljava/lang/String;" + environmentClass.descriptor() + ")V");
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitInsn(Opcodes.RETURN);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this",Naming.internalToDesc(className), null, l0, l2, 0);
        mv.visitLocalVariable("name", "Ljava/lang/String;", null, l0, l2, 1);
        // See comment above on ClassWriter.COMPUTE_FRAMES
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

    private static void writeDumpMethod(ClassWriter cw, String className, EnvSymbolNames symbolNames) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "dump", "(Ljava/lang/Appendable;)Ljava/lang/Appendable;", null, new String[]{"java/io/IOException"});
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
        mv.visitLocalVariable("this",Naming.internalToDesc(className), null, l0, l9, 0);
        mv.visitLocalVariable("a", "Ljava/lang/Appendable;", null, l0, l9, 1);
        // See comment above on ClassWriter.COMPUTE_FRAMES
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private static int dumpFields(MethodVisitor mv, String className, EnvironmentClass eClass, EnvSymbolNames symbolNames, int linebreaks) {
        for (String fieldName : symbolNames.getSymbolNames(eClass)) {
            Label l6 = new Label();
            mv.visitLabel(l6);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn("(" + fieldName + " = ");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/Appendable", "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
            mv.visitInsn(Opcodes.POP);
            Label l7 = new Label();
            mv.visitLabel(l7);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            String idString = fieldName + eClass.namespace();
            mv.visitFieldInsn(Opcodes.GETFIELD, className, Naming.mangleIdentifier(idString), eClass.descriptor());
            Label l8 = new Label();
            mv.visitJumpInsn(Opcodes.IFNULL, l8);
            Label l9 = new Label();
            mv.visitLabel(l9);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, Naming.mangleIdentifier(idString), eClass.descriptor());
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, eClass.internalName(), "toString", "()Ljava/lang/String;");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/Appendable", "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
            mv.visitInsn(Opcodes.POP);
            Label afterNull = new Label();
            mv.visitJumpInsn(Opcodes.GOTO, afterNull);
            mv.visitLabel(l8);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn("null");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/Appendable", "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
            mv.visitInsn(Opcodes.POP);
            mv.visitLabel(afterNull);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn(") ");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/Appendable", "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
            mv.visitInsn(Opcodes.POP);
            linebreaks = (linebreaks + 1) % 5;
            if (linebreaks == 0) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitLdcInsn("\n");
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/Appendable", "append", "(Ljava/lang/CharSequence;)Ljava/lang/Appendable;");
                mv.visitInsn(Opcodes.POP);
            }
        }

        return linebreaks;
    }

    private static void outputClassFiles(Map<APIName, Pair<String, byte[]>> compiledCompUnits, String classSuffix, HashSet<StaticError> errors) {
        for (Map.Entry<APIName, Pair<String, byte[]>> component : compiledCompUnits.entrySet()) {
            Pair<String, byte[]> compOutput = component.getValue();
            String fileName = ProjectProperties.ENVIRONMENT_CACHE_DIR + File.separator + compOutput.getA() + ".class";
            outputClassFile(compOutput.getB(), fileName, errors);
        }
    }

    /**
     * Given a Java bytecode class stored in a byte array, save that
     * class into a file on disk.
     *
     * @throws IOException
     */
    private static void outputClassFile(byte[] bytecode, String fileName, HashSet<StaticError> errors) {
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(new File(fileName));
            outStream.write(bytecode);
        }
        catch (IOException e) {
            errors.add(new WrappedException(e));
        }
        finally {
            try {
                if (outStream != null) outStream.close();
            }
            catch (IOException e) {
                errors.add(new WrappedException(e));
            }
        }
    }

}
