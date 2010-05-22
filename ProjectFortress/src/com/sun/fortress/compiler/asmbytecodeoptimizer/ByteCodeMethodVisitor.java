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
import java.util.HashMap;
import java.util.jar.*;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class ByteCodeMethodVisitor extends AbstractVisitor implements MethodVisitor {

    public HashMap labelNames;
    public ArrayList<Insn> insns;
    int access;
    public String name;
    String desc;
    String sig;
    String[] exceptions;

    static int INVOKESTATIC = Opcodes.INVOKESTATIC;
    static int GETSTATIC = Opcodes.GETSTATIC;
    static int INVOKEVIRTUAL = Opcodes.INVOKEVIRTUAL;
    static int INVOKEINTERFACE = Opcodes.INVOKEINTERFACE;

    public ByteCodeMethodVisitor(int access, String name, String desc, String sig, String[] exceptions) {
        this.labelNames = new HashMap();
        this.insns = new ArrayList<Insn>();
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.sig = sig;
        this.exceptions = exceptions;
    }

    public void toAsm(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(access, name, desc, sig, exceptions);
        for (Insn i : insns) {
            i.toAsm(mv);
        }
    }

    public void print() {
        System.out.println("Method " + name + " desc = " + desc + " sig = " + sig);
        for (Insn i : insns) {
            System.out.println(i);
        }
    }

    public AnnotationVisitor visitAnnotationDefault() {
        System.out.println("visitAnnotationDefault");
        return new ByteCodeAnnotationVisitor(0);        
    }

    public AnnotationVisitor visitAnnotation(String s, boolean b) {
        System.out.println("visitAnnotation");
        return new ByteCodeAnnotationVisitor(0);        
    }


    public void visitAttribute(Attribute attr) {
        System.out.println("visitAttribute");
    }

    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        System.out.println("visitParameterAnnotation");
        return new ByteCodeAnnotationVisitor(0);
    }

    public void visitCode() {

    }
        
    public void visitFrame(int type, int nLocal, Object local[], int nStack, Object stack[]) {

    }
        
    public void visitInsn(int opcode) {
        insns.add(new SingleInsn(OPCODES[opcode], opcode));
    }

    public void visitIntInsn(int opcode, int operand) {
        insns.add(new IntInsn(OPCODES[opcode], opcode, operand));
    }

    public void visitVarInsn(int opcode, int var) {
        insns.add(new VarInsn(OPCODES[opcode], opcode, var));
    }
    
    public void visitTypeInsn(int opcode, String type) {
        insns.add(new TypeInsn(OPCODES[opcode], opcode, type));
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        insns.add(new FieldInsn(OPCODES[opcode], opcode, owner, name, desc));
    }

    public void visitJumpInsn(int opcode, Label label) {
        insns.add(new JumpInsn(OPCODES[opcode], opcode, label));
    }

    public void visitLabel(Label label) {
        labelNames.put(label.toString(), Integer.valueOf(insns.size()));
        insns.add(new LabelInsn("Label", label));
    }

    public void visitLdcInsn(Object cst) {
        insns.add(new LdcInsn("LdcInsn", cst));
    }

    public void visitIincInsn(int var, int increment) {
        insns.add(new IincInsn("IincInsn", var, increment));
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
        insns.add(new TableSwitchInsn("TableSwitchInsn", min, max, dflt, labels));
    }

    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        insns.add(new LookupSwitchInsn("LookupSwitchInsn", dflt, keys, labels));
    }

    public void visitMultiNewArrayInsn(String desc, int dims) {
        System.out.println("visitMultiNewArrayInsn");
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        insns.add(new NotYetImplementedInsn("visitTryCatchBlock"));
    }

    public void visitLocalVariable(String name, String desc, String sig, Label start, Label end, int index) {
        insns.add(new LocalVariable("visitLocalVariable", name, desc, sig, start, end, index));
    }

    public void visitMultiANewArrayInsn(String name, int i) {
        insns.add(new NotYetImplementedInsn("visitMultiANewArrayInsn"));
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        insns.add(new MethodInsn(OPCODES[opcode], opcode, owner, name, desc));
    }

    public void visitLineNumber(int line, Label start) {
        insns.add(new VisitLineNumberInsn("visitLineNumber", line, start));
    }

    public void visitMaxs(int maxStack, int maxLocals) {
        insns.add(new VisitMaxs("visitMaxs", maxStack, maxLocals));
    }

    public void visitEnd() {
        insns.add(new VisitEnd("visitEnd"));
    }
    

}

