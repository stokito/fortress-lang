/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.ByteCodeWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.jar.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.*;
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
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES); 
        //        ClassWriter cw = new ClassWriter(0);
        CheckClassAdapter cca = new CheckClassAdapter(cw);

        cca.visit(version, access, name, sig, superName, interfaces);
        Iterator it = fieldVisitors.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            ByteCodeFieldVisitor fv = (ByteCodeFieldVisitor) pairs.getValue();
            fv.toAsm(cca);
        }

        it = methodVisitors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            ByteCodeMethodVisitor mv = (ByteCodeMethodVisitor) pairs.getValue();
            mv.toAsm(cca);
        }

        byte [] bytes = cw.toByteArray();
        if (ProjectProperties.getBoolean("fortress.bytecode.verify", false)) {
            PrintWriter pw = new PrintWriter(System.out);
            CheckClassAdapter.verify(new ClassReader(bytes), true, pw);
        }
        System.out.println("About to write " + name);
        ByteCodeWriter.writeJarredClass(jos, name, bytes);
    }

        

    public void visit(int version, int access, String _name, String sig, String superName, String[] interfaces) {
        this.version = version;
        this.access = access;
        this.name = _name;
        this.sig = sig;
        this.superName = superName;
        this.interfaces = interfaces;
    }

    public void visitSource(String file, String debug) {
        //this.sourceFile = file;
    }

    public void visitOuterClass(String owner, String _name, String desc) {
    }

    public void visitInnerClass(String _name, String outerName, String innerName, int access) {
    }

    public FieldVisitor visitField(int access, String _name, String desc, String sig, Object value) {
        ByteCodeFieldVisitor bcfv = new ByteCodeFieldVisitor(access, _name, desc, sig, value);
        fieldVisitors.put(_name, bcfv);
        return bcfv;
    }

    public MethodVisitor visitMethod(int access, String _name, String desc, String sig, String[] exceptions) {
        ByteCodeMethodVisitor bcmv = new ByteCodeMethodVisitor(access, _name, desc, sig, exceptions);
        methodVisitors.put(_name + desc, bcmv);
        bcmv.print();
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
