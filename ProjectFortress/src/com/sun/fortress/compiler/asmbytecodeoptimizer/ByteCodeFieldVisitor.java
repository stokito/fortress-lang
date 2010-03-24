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

import java.io.*;
import java.util.ArrayList;
import java.util.jar.*;
import java.util.List;

import org.objectweb.asm.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.*;

public class ByteCodeFieldVisitor implements FieldVisitor {
    int access;
    String name;
    String desc;
    String sig;
    Object value;


    ByteCodeFieldVisitor(int access, String name, String desc, String sig, Object value) {
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.sig = sig;
        this.value = value;
    }

    public void print() {
        System.out.println("Field " + name + " desc = " + desc + " value = " + value);
    }

    public void toAsm(ClassWriter cw) {
        cw.visitField(access, name, desc, sig, value);
    }


    public void visitAttribute(Attribute attr) {
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return new ByteCodeAnnotationVisitor(0);
    }

    public void visitEnd() {
    }
        
} 

