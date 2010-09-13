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

import com.sun.fortress.compiler.NamingCzar;

public class ByteCodeMethodVisitor extends AbstractVisitor implements MethodVisitor {

    public HashMap labelNames;
    public ArrayList<Insn> insns;
    int access;
    public String name;
    String desc;
    String sig;
    String[] exceptions;
    List<String> args;
    String result; 
    int maxStack;
    int maxLocals;
    boolean changed;

    // Is useful for debugging

    static boolean noisy = false;

    void addInsn(Insn i) {
        insns.add(i);
    }

    public ByteCodeMethodVisitor(int access, String name, String desc, String sig, String[] exceptions) {
        this.labelNames = new HashMap();
        this.insns = new ArrayList<Insn>();
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.sig = sig;
        this.exceptions = exceptions;
        this.args = NamingCzar.parseArgs(desc);
        this.result = NamingCzar.parseResult(desc);
        changed = false;
    }

    public boolean isStaticMethod() {return ((access & Opcodes.ACC_STATIC) > 0);}

    public boolean isAbstractMethod() {return ((access & Opcodes.ACC_ABSTRACT) > 0); }

    public void toAsm(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(access, name, desc, sig, exceptions);
        for (Insn i : insns) {
            i.toAsmWrapper(mv);
        }
    }

    public String toString() {
        return "Method " + name + " desc = " + desc + " sig = " + sig;
    }

    public void print() {
        if (noisy) {
            System.out.println("Method " + name + " desc = " + desc + " sig = " + sig);
            System.out.println("BCMV = " + this);
            System.out.println("Args = " + args);
            System.out.println("result = " + result);

            for (Insn i : insns) {
                System.out.println(i.toString());
            }
        }
        
    }

    public AnnotationVisitor visitAnnotationDefault() {
        return new ByteCodeAnnotationVisitor(0);        
    }

    public AnnotationVisitor visitAnnotation(String s, boolean b) {
        return new ByteCodeAnnotationVisitor(0);        
    }


    public void visitAttribute(Attribute attr) {
    }

    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        return new ByteCodeAnnotationVisitor(0);
    }

    public void visitCode() {
    }
        
    public void visitFrame(int type, int nLocal, Object local[], int nStack, Object stack[]) {
        addInsn(new VisitFrame(type, nLocal, local, nStack, stack));
    }
        
    public void visitInsn(int opcode) {
        addInsn(new SingleInsn(OPCODES[opcode], opcode));
    }

    public void visitIntInsn(int opcode, int operand) {
        addInsn(new IntInsn(OPCODES[opcode], opcode, operand));
    }

    public void visitVarInsn(int opcode, int var) {
        addInsn(new VarInsn(OPCODES[opcode], opcode, var));
    }
    
    public void visitTypeInsn(int opcode, String type) {
        addInsn(new TypeInsn(OPCODES[opcode], opcode, type));
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        addInsn(new FieldInsn(OPCODES[opcode], opcode, owner, name, desc));
    }

    public void visitJumpInsn(int opcode, Label label) {
        addInsn(new JumpInsn(OPCODES[opcode], opcode, label));
    }

    public void visitLabel(Label label) {
        labelNames.put(label.toString(), Integer.valueOf(insns.size()));
        addInsn(new LabelInsn("Label", label));
    }

    public void visitLdcInsn(Object cst) {
        addInsn(new LdcInsn("LdcInsn", cst));
    }

    public void visitIincInsn(int var, int increment) {
        addInsn(new IincInsn("IincInsn", var, increment));
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
        addInsn(new TableSwitchInsn("TableSwitchInsn", min, max, dflt, labels));
    }

    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        addInsn(new LookupSwitchInsn("LookupSwitchInsn", dflt, keys, labels));
    }

    public void visitMultiNewArrayInsn(String desc, int dims) {
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        addInsn(new NotYetImplementedInsn("visitTryCatchBlock"));
    }

    public void visitLocalVariable(String name, String desc, String sig, Label start, Label end, int index) {
        addInsn(new LocalVariableInsn("visitLocalVariable", name, desc, sig, start, end, index));
    }

    public void visitMultiANewArrayInsn(String name, int i) {
        addInsn(new NotYetImplementedInsn("visitMultiANewArrayInsn"));
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        addInsn(new MethodInsn(OPCODES[opcode], opcode, owner, name, desc));
    }

    public void visitLineNumber(int line, Label start) {
        addInsn(new VisitLineNumberInsn("visitLineNumber", line, start));
    }

    public void visitMaxs(int maxStack, int maxLocals) {
        this.maxStack = maxStack;
        this.maxLocals = maxLocals;
        addInsn(new VisitMaxs("visitMaxs", maxStack, maxLocals));
    }

    public void visitEnd() {
        addInsn(new VisitEnd("visitEnd"));
    }
    

}

