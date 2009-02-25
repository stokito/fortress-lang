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
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.useful.Debug;

class fortressConverter {
    String shortName;
    String fortressRuntimeType;
    String toJavaTypeMethod;
    String toJavaTypeMethodDesc;
    String constructor;
    String constructorType;

    private final String prefix = "com/sun/fortress/interpreter/evaluator/values/";

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

    String className = "temp";
    private final String prefix = "com/sun/fortress/interpreter/evaluator/values/";
    private HashMap conversionTable;

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
        initializeEntry("FString", "toString", "()Ljava/lang/String;", "<init>", "L(java/lang/String;)LFstring;");
        initializeEntry("FLong", "getLong", "()J", "make", "(J)L" + prefix + "FLong;");
        initializeEntry("FInt", "getInt", "()I", "make", "(I)L" + prefix + "FInt;");
        initializeEntry("FFloat", "getFloat", "()D", "make", "(D)L" + prefix + "FFloat;");
        initializeEntry("FRR32", "getRR32", "()F", "make", "(F)L" + prefix + "FRR32;");
        initializeEntry("FBool", "getBool", "()Z", "make", "(Z)L" + prefix + "FBool;");
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

    public FortressMethodAdapter(ClassVisitor cv, String outputClassName) {
        super(cv);
        className = outputClassName.replace('.','/');
        initializeTables();
    }

    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        Debug.debug( Debug.Type.COMPILER, 1,
                     "visit:" + name + " generate " + className);
        cv.visit(version, access, className, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(int access,
                                     String name, String desc,
                                     String signature, String[] exceptions) {
        // Don't know how to do these, or if we need them...
        if (name.equals("<init>") || name.equals("<clinit>"))
            Debug.debug( Debug.Type.COMPILER, 1, "Don't visit Method " + name);
        else if (SignatureParser.unsayable(desc))
            Debug.debug( Debug.Type.COMPILER, 1,
                         "Don't visit Method with unsayable desc" + name);
        else {
            generateNewBody(access, desc, signature, exceptions, name, name);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }


    private void generateNewBody(int access,
                                 String desc, String signature,
                                 String[] exceptions,
                                 String name, String newName) {

        SignatureParser sp = new SignatureParser(desc);
        MethodVisitor mv = cv.visitMethod(access, name,
                                          sp.getFortressifiedSignature(),
                                          signature, exceptions);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        int count = 0;
        List<String> args = sp.getFortressArguments();
        for (String s : args) {
            mv.visitVarInsn(Opcodes.ALOAD, count++);
            String stripped = strip(s);
            fortressConverter converter = (fortressConverter) conversionTable.get(stripped);
            if (converter == null)
                throw new RuntimeException("Can't generate header for method " + name + " problem =" + s);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                               prefix + stripped,
                               converter.toJavaTypeMethod,
                               converter.toJavaTypeMethodDesc);
        }

        Debug.debug( Debug.Type.COMPILER, 1,
                     "className = " + className + " name = " + name + " access = " + access);

        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                           className,
                           name,
                           sp.getSignature());

        String result = sp.getFortressResult();
        String stripped = strip(result);

        if (result.equals("L" + prefix + "FVoid;")) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, prefix + "FVoid",
                              "V", "L" + prefix + "FVoid;");
        } else {
            fortressConverter converter = (fortressConverter) conversionTable.get(stripped);
            if (converter == null)
                throw new RuntimeException("Can't generate return type for method " + name + " value " + result);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, prefix + strip(result),
                               converter.constructor,
                               converter.constructorType);
        }

        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(2,1);
        mv.visitEnd();
    }

    public void visitEnd() {
        super.visitEnd();
    }
}
