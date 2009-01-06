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

import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.interpreter.evaluator.values.FLong;


public class FortressMethodAdapter extends ClassAdapter {

    String className = "temp";
    private final String prefix = "com/sun/fortress/interpreter/evaluator/values/";

    public FortressMethodAdapter(ClassVisitor cv) {
        super(cv);
    }

    public void visit(int version, int access, String name, String signature, 
                      String superName, String[] interfaces) {
        cv.visit(version, access, name, signature, superName, interfaces);
        className = name;
    }

    public MethodVisitor visitMethod(int access, 
                                     String name, String desc, 
                                     String signature, String[] exceptions) {
        if (!name.equals("<init>")) {
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
            if (s.equals("L" + prefix + "FString;")) {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, prefix + "FString", "toString", 
                                   "()Ljava/lang/String;");
            } else if (s.equals("L" + prefix + "FLong")) {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, prefix + "FLong", "toLong",
                                   "()J");
            } else if (s.equals("L" + prefix + "FInt;")) {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, prefix + "FInt", "toInt",
                                   "()I");
            }
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                           className,
                           name,
                           sp.getSignature());

        //     mv.visitVarInsn(Opcodes.LSTORE, count);
        if (sp.getFortressResult().equals("L" + prefix + "FString;")) 
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, prefix + "FString", 
                               "<init>", "(Ljava/lang/String;)LFString;");
        else if (sp.getFortressResult().equals("L" + prefix + "FLong;"))
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, prefix + "FLong",
                               "<init>", "(J)LFLong;");
        else if (sp.getFortressResult().equals("L" + prefix + "FInt;"))
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "FInt",
                               "<init>", "(I)LFInt;");
        else if (sp.getFortressResult().equals("L" + prefix + "FVoid;")) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, prefix + "FVoid",
                              "V", "L" + prefix + "FVoid;");
        }
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(2,1);
        mv.visitEnd();
    }

    public void visitEnd() {
        super.visitEnd();
    }
}