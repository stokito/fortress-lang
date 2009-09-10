/*******************************************************************************
  Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.nativeInterface;

import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.EmptyVisitor;

import com.sun.fortress.compiler.codegen.CodeGen;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.OverloadSet;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;
import com.sun.fortress.nativeHelpers.*;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.MultiMap;

class fortressConverter {
    String shortName;
    String fortressRuntimeType;
    String toJavaTypeMethod;
    String toJavaTypeMethodDesc;
    String constructor;
    String constructorType;

    private final String prefix = "com/sun/fortress/compiler/runtimeValues";

    fortressConverter(String _fortressRuntimeType,
                      String _toJavaTypeMethod,
                      String _toJavaTypeMethodDesc,
                      String _constructor,
                      String _constructorType) {

        shortName = _fortressRuntimeType;
        fortressRuntimeType = "L" + prefix + _fortressRuntimeType + ";";
        toJavaTypeMethod = _toJavaTypeMethod;
        toJavaTypeMethodDesc = _toJavaTypeMethodDesc;
        constructor = _constructor;
        constructorType = _constructorType;
    }

}

public class FortressMethodAdapter extends ClassAdapter {

    String inputClassName;
    String outputClassName;
    private final String prefix = "com/sun/fortress/compiler/runtimeValues/";
    private final String prefixDotted = "com.sun.fortress.compiler.runtimeValues";
    private HashMap conversionTable;

    private APIName apiName;
    private Map<IdOrOpOrAnonymousName,MultiMap<Integer, Function>> sizePartitionedOverloads;
    private TypeAnalyzer ta;
    private ClassWriter cw;
    private Set<String> overloadedNamesAndSigs;

    private void initializeEntry(String fortressRuntimeType,
                                 String toJavaTypeMethod,
                                 String toJavaTypeMethodDesc,
                                 String constructor,
                                 String constructorType) {
        conversionTable.put(fortressRuntimeType,
                            new fortressConverter(fortressRuntimeType,
                                                  toJavaTypeMethod,
                                                  toJavaTypeMethodDesc,
                                                  constructor,
                                                  constructorType));
    }

    private void initializeTables() {
        conversionTable = new HashMap();
        initializeEntry("FZZ32",   "getValue", "()I", "make", "(I)L" + prefix + "FZZ32;");
        initializeEntry("FZZ64",   "getValue", "()J", "make", "(J)L" + prefix + "FZZ64;");
        initializeEntry("FRR32",   "getValue", "()F", "make", "(F)L" + prefix + "FRR32;");
        initializeEntry("FRR64",   "getValue", "()D", "make", "(D)L" + prefix + "FRR64;");
        initializeEntry("FBoolean",   "getValue", "()Z", "make", "(Z)L" + prefix + "FBoolean;");
        initializeEntry("FVoid",   "getValue", "()",  "make", "()L" + prefix + "FVoid;");
        initializeEntry("FString", "getValue", "()Ljava/lang/String;", "make", "(Ljava/lang/String;)L" +
                        prefix + "FString;");
    }

    // Strip off the leading L + prefix, and trailing ;"

    private String strip(String s) {
        String header = "L" + prefix;
        String trailer = ";";
        int end = s.lastIndexOf(";");
        int start = header.length();
        return s.substring(start,end);
    }

    private String addWrap(String s) {
        return "L" + prefix + s + ";" ;
    }

    public FortressMethodAdapter(ClassWriter cv,
            String inputClassName,
            String outputClassName,
            APIName api_name,
            Map<IdOrOpOrAnonymousName,MultiMap<Integer, Function>> size_partitioned_overloads,
            TypeAnalyzer ta) {
        super(cv);
        this.cw = cv;
        this.inputClassName = inputClassName.replace('.','/');
        this.outputClassName = outputClassName.replace('.','/');
        this.apiName = api_name;
        this.sizePartitionedOverloads = size_partitioned_overloads;
        this.ta = ta;
        initializeTables();
    }

    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        Debug.debug( Debug.Type.COMPILER, 1,
                     "visit:" + name + " generate " + inputClassName);
        cv.visit(version, access, outputClassName, signature, superName, interfaces);

        overloadedNamesAndSigs = CodeGen.generateTopLevelOverloads(apiName, sizePartitionedOverloads, ta, cw);
    }

    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        // Don't know how to do these, or if we need them...
        if (name.equals("<init>") || name.equals("<clinit>"))
            Debug.debug(Debug.Type.COMPILER, 1, "Don't visit Method ", name);
        else if (SignatureParser.unsayable(desc))
            Debug.debug(Debug.Type.COMPILER, 1,
                    "Don't visit Method with unsayable desc", name);
        else {

            generateNewBody(access, desc, signature, exceptions, name, name);

        }

        return new EmptyVisitor();// super.visitMethod(access, name, desc, signature, exceptions);
    }

    private MethodVisitor generateNewBody(int access, String desc, String signature,
            String[] exceptions, String name, String newName) {

        Debug.debug(Debug.Type.COMPILER, 1, "generateNewBody: ", name,
                    " with desc ", desc);

        SignatureParser sp = new SignatureParser(desc);
        String fsig = sp.getFortressifiedSignature();
        MethodVisitor mv = cv.visitMethod(access, name, fsig, signature,
                exceptions);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        int count = 0;
        List<String> args = sp.getFortressArguments();
        for (String s : args) {
            mv.visitVarInsn(Opcodes.ALOAD, count++);
            String stripped = strip(s);
            fortressConverter converter = (fortressConverter) conversionTable
                    .get(stripped);
            if (converter == null)
                throw new RuntimeException("Can't generate header for method "
                        + name + " problem =" + s + " stripped = " + stripped);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, prefix + stripped,
                    converter.toJavaTypeMethod, converter.toJavaTypeMethodDesc);
        }

        Debug.debug(Debug.Type.COMPILER, 1, "className = ", inputClassName,
                    " name = ", name, " access = ", access);

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, inputClassName, name, sp
                .getSignature());

        String result = sp.getFortressResult();
        String stripped = strip(result);

        fortressConverter converter = (fortressConverter) conversionTable
                .get(stripped);
        if (converter == null)
            throw new RuntimeException("Can't generate return type for method "
                    + name + " value " + result);

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, prefix + strip(result),
                converter.constructor, converter.constructorType);

        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
        return mv;
    }

    public void visitEnd() {

        super.visitEnd();
    }


}
