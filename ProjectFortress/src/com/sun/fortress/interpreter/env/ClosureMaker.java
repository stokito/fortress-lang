/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.env;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.environments.SimpleClassLoader;
import static com.sun.fortress.exceptions.ProgramError.error;
import com.sun.fortress.nodes.*;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.ByteCodeWriter;
import com.sun.fortress.useful.NotFound;
import com.sun.fortress.useful.Useful;
import org.objectweb.asm.*;
import org.objectweb.asm.Label;

public class ClosureMaker implements Opcodes {

    public static Applicable closureForTopLevelFunction(APIName apiname, FnDecl fd) throws InstantiationException,
                                                                                           IllegalAccessException,
                                                                                           Exception {
        String pkg_dots = NamingCzar.apiNameToPackageName(apiname);
        String pkg_slashes = Useful.replace(pkg_dots, ".", "/");
        Id name = (Id) fd.getHeader().getName();
        String aClass;
        String aMethod;
        try {
            aClass = Useful.extractBeforeMatch(name.getText(), ".");
            aMethod = Useful.extractAfterMatch(name.getText(), ".");
        }
        catch (NotFound nf) {
            return error("Foreign import name " + name.getText() + " ought to have a dot in it");
        }

        String classname_for_our_wrapper = pkg_slashes + "/" + aClass + "$$closure";
        // As far as I can imagine at this point, all Java names are Ids
        byte[] bytecodes = forTopLevelFunction(apiname, fd, classname_for_our_wrapper, aClass, aMethod);
        ByteCodeWriter.writeClass(bytecodes,
                                  ProjectProperties.BYTECODE_CACHE_DIR + "/" + classname_for_our_wrapper + ".class");
        return (Applicable) SimpleClassLoader.defineAsNecessaryAndAllocate(classname_for_our_wrapper, bytecodes);
    }

    public static byte[] forTopLevelFunction(APIName apiname,
                                             FnDecl fd,
                                             String closureClass,
                                             String aClass,
                                             String aMethod) throws Exception {

        //IdOrOp ua_name = fd.getUnambiguousName();
        /*
         This is a cheat; ought to be more algorithmic, so this will
         work in general, not just for native wrappers.  Proper
         algorithm derives it from the FnDecl.
         */
        String md = ForeignJava.only.methodToDecl.inverse().get(fd);

        /*
         translate apiname into package name
         extract aClass from name
         md is the method name.

         need to generate a class, extending
         com.sun.fortress.interpreter.Glue.NativeFn#,
         where # is the number of parameters in the decl,

         that class must contain a method (say, #=3)
          FValue applyToArgs(FValue x, FValue y, FValue z) {
            return package.class.md(x,y,z);
          }
         and that is all.
        */
        String pkg_dots = NamingCzar.apiNameToPackageName(apiname);
        String pkg_slashes = Useful.replace(pkg_dots, ".", "/");
        int nargs = fd.getHeader().getParams().size();

        String nativeHelper = "com/sun/fortress/interpreter/glue/NativeFn" + nargs;
        String nativeWrapperClass = pkg_slashes + "/" + aClass;
        String LclosureClass = "L" + closureClass + ";";
        String fvalue = "Lcom/sun/fortress/interpreter/evaluator/values/FValue;";

        String signature = "(";
        StringBuilder buf = new StringBuilder();
        buf.append(signature);
        for (int i = 1; i <= nargs; i++) {
            buf.append(fvalue);
        }
        signature = buf.toString();
        signature += ")" + fvalue;

        System.err.println(md);


        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, closureClass, null, nativeHelper, null);

        cw.visitSource(null, null);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, nativeHelper, "<init>", "()V");
            mv.visitInsn(RETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", LclosureClass, null, l0, l1, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PROTECTED, "applyToArgs", signature, null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            for (int i = 1; i <= nargs; i++) {
                mv.visitVarInsn(ALOAD, i);
            }
            mv.visitMethodInsn(INVOKESTATIC, nativeWrapperClass, aMethod, signature);
            mv.visitInsn(ARETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitLocalVariable("this", LclosureClass, null, l0, l1, 0);
            for (int i = 1; i <= nargs; i++) {
                mv.visitLocalVariable("p" + i, fvalue, null, l0, l1, 1);
            }
            mv.visitMaxs(nargs, nargs + 1);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

}
