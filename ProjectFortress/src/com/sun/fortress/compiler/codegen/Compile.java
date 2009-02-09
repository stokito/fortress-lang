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
package com.sun.fortress.compiler.codegen;


import com.sun.fortress.compiler.nativeInterface.MyClassLoader;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor_void;

import java.util.*;
import org.objectweb.asm.*;


public class Compile extends NodeAbstractVisitor_void {
    ClassWriter cw;
    FieldVisitor fv;
    MethodVisitor mv;
    AnnotationVisitor av0;
    String className;
    MyClassLoader loader;
    CanCompile c;

    private void generateMainMethod() {
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "run", "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0,1);
        mv.visitEnd();

    }

    private void generateInitMethod() {
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private void generateRunMethod() {
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "run", "()V", null, null);
        mv.visitCode();
        mv.visitTypeInsn(Opcodes.NEW, className);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, className, "<init>", "()V");
        mv.visitVarInsn(Opcodes.ASTORE, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "test", "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    private void generateTestMethod() {
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "test", "()V", null, null);
        mv.visitCode();
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System","out","Ljava/io/PrintStream;");
        mv.visitLdcInsn("This is just a test header");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2,1);
        mv.visitEnd();
    }

    public Compile(String n) {
        loader = new MyClassLoader();
        cw = new ClassWriter(0);
        className = n;
        c = new CanCompile();

        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, className, null, "java/lang/Object", null);
        generateInitMethod();
        generateRunMethod();
        generateTestMethod();
        generateMainMethod();
    }

    public void dumpClass() {
        cw.visitEnd();
        loader.writeClass(className, cw.toByteArray());
    }

    private void sayWhat(Node that) {
        throw new RuntimeException("Can't compile " + that);
    }

    public void defaultCase(Node that) {
        // System.out.println("defaultCase" + that);
        sayWhat(that);
    }

    public void forFnDecl(FnDecl x) {
        if (x.accept(c)) {
            try {
                // System.out.println("forFnDecl " + x);
                //         dump(x);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else
            sayWhat(x);
    }

    public void forComponent(Component x) {
        CanCompile c = new CanCompile();
        if (x.accept(c)) {
            // System.out.println("About to compile Component x");
            List<Decl> decls = x.getDecls();
            for (Decl d : decls) {
                forDecl(d);
            }
        }
    }

    public void forDecl(Decl x) {
        if (x instanceof FnDecl)
            forFnDecl((FnDecl) x);
        else {
            throw new RuntimeException("Only know how to compile FnDecls");
        }
    }
}
