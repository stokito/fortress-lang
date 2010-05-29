/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import com.sun.fortress.compiler.ByteCodeWriter;
import com.sun.fortress.repository.ProjectProperties;

import java.io.*;
import java.util.ArrayList;
import java.util.jar.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.*;


class ByteCodeVisitor implements ClassVisitor {

    HashMap methodVisitors = new HashMap();
    HashMap fieldVisitors = new HashMap();

    int version;
    int access;
    String name;
    String sig;
    String superName;
    String[] interfaces;

    //String sourceFile;

    public void print() {
        Iterator it = fieldVisitors.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            System.out.println("Printing Field: " + pairs.getKey() + ":");
            ByteCodeFieldVisitor fv = (ByteCodeFieldVisitor) pairs.getValue();
            fv.print();
        }

        it = methodVisitors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            ByteCodeMethodVisitor bcmv = (ByteCodeMethodVisitor) pairs.getValue();
            System.out.println("Printing Method: Key = " + pairs.getKey() + " bcmv says " + bcmv.name);

            bcmv.print();
        }

    }

    public void toAsm(JarOutputStream jos) {
        ClassWriter cw = new ClassWriter(1);
        cw.visit(version, access, name, sig, superName, interfaces);
        Iterator it = fieldVisitors.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            ByteCodeFieldVisitor fv = (ByteCodeFieldVisitor) pairs.getValue();
            fv.toAsm(cw);
        }

        it = methodVisitors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            ByteCodeMethodVisitor mv = (ByteCodeMethodVisitor) pairs.getValue();
            mv.toAsm(cw);
        }

        byte [] bytes = cw.toByteArray();
        if (ProjectProperties.getBoolean("fortress.bytecode.verify", false)) {
            PrintWriter pw = new PrintWriter(System.out);
            CheckClassAdapter.verify(new ClassReader(bytes), true, pw);
        }

        ByteCodeWriter.writeJarredClass(jos, name, bytes);
        System.out.println("Wrote class " + name);
    }

        

    public void visit(int version, int access, String name, String sig, String superName, String[] interfaces) {
        this.version = version;
        this.access = access;
        this.name = name;
        this.sig = sig;
        this.superName = superName;
        this.interfaces = interfaces;
    }

    public void visitSource(String file, String debug) {
        //this.sourceFile = file;
    }

    public void visitOuterClass(String owner, String name, String desc) {
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
    }

    public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
        ByteCodeFieldVisitor bcfv = new ByteCodeFieldVisitor(access, name, desc, sig, value);
        fieldVisitors.put(name, bcfv);
        return bcfv;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
        System.out.println("visitMethod: className = " + this.name + " name = " + name + " desc = " + desc);
        ByteCodeMethodVisitor bcmv = new ByteCodeMethodVisitor(access, name, desc, sig, exceptions);
        methodVisitors.put(name + desc, bcmv);
        bcmv.print();
        System.out.println("VisitMethod " + name + " with bcmv " + bcmv);
        return bcmv;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return new ByteCodeAnnotationVisitor(0);
    }

    public void visitAttribute(Attribute attr) {
    }

    public void visitEnd() {
    }
}