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
import com.sun.fortress.runtimeSystem.Naming;

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

    int index;

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
        this.index = 0;
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

    public void toAsm(CheckClassAdapter cca) {
        MethodVisitor mv = cca.visitMethod(access, name, desc, sig, exceptions);
        for (Insn i : insns) {
            i.toAsmWrapper(mv);
        }
    }

    public String toString() {
        return "Method " + name + " desc = " + desc + " sig = " + sig;
    }

    public void printInsns(List<Insn> instructions, String header) {
        for (Insn i : instructions) {
            if (i.isExpanded()) {
                System.out.println("This instruction was expanded: "+ i.toString());
                printInsns(i.inlineExpansionInsns, header + "   ");
            } else System.out.println(header + i.toString());
        }
    }
        

    public void print() {
        if (noisy) {
            System.out.println("Method " + name + " desc = " + desc + " sig = " + sig);
            System.out.println("BCMV = " + this);
            System.out.println("Args = " + args);
            System.out.println("result = " + result);
            printInsns(insns, "");
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
        addInsn(new VisitCode(Integer.toString(index++)));  
    }
        
    public void visitFrame(int type, int nLocal, Object local[], int nStack, Object stack[]) {
        addInsn(new VisitFrame(type, nLocal, local, nStack, stack, Integer.toString(index++)));
    }
        
    public void visitInsn(int opcode) {
        addInsn(new SingleInsn(OPCODES[opcode], opcode, Integer.toString(index++)));
    }

    public void visitIntInsn(int opcode, int operand) {
        addInsn(new IntInsn(OPCODES[opcode], opcode, operand, Integer.toString(index++)));
    }

    public void visitVarInsn(int opcode, int var) {
        addInsn(new VarInsn(OPCODES[opcode], opcode, var, Integer.toString(index++)));
    }
    
    public void visitTypeInsn(int opcode, String type) {
        addInsn(new TypeInsn(OPCODES[opcode], opcode, type, Integer.toString(index++)));
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        addInsn(new FieldInsn(OPCODES[opcode], opcode, owner, name, desc, Integer.toString(index++)));
    }

    public void visitJumpInsn(int opcode, Label label) {
        addInsn(new JumpInsn(OPCODES[opcode], opcode, label, Integer.toString(index++)));
    }

    public void visitLabel(Label label) {
        labelNames.put(label.toString(), index);
        addInsn(new LabelInsn("Label", label, Integer.toString(index++)));
    }

    public void visitLdcInsn(Object cst) {
        addInsn(new LdcInsn("LdcInsn", cst, Integer.toString(index++)));
    }

    public void visitIincInsn(int var, int increment) {
        addInsn(new IincInsn("IincInsn", var, increment, Integer.toString(index++)));
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
        addInsn(new TableSwitchInsn("TableSwitchInsn", min, max, dflt, labels, Integer.toString(index++)));
    }

    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        addInsn(new LookupSwitchInsn("LookupSwitchInsn", dflt, keys, labels, Integer.toString(index++)));
    }

    public void visitMultiNewArrayInsn(String desc, int dims) {
        addInsn(new NotYetImplementedInsn("visitMultiNewArrayInsn", Integer.toString(index++)));
        index++;
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        addInsn(new NotYetImplementedInsn("visitTryCatchBlock", Integer.toString(index++)));
    }

    public void visitLocalVariable(String name, String desc, String sig, Label start, Label end, int _index) {
        addInsn(new LocalVariableInsn("visitLocalVariable", name, desc, sig, start, end, _index, Integer.toString(index++)));
    }

    public void visitMultiANewArrayInsn(String name, int i) {
        addInsn(new NotYetImplementedInsn("visitMultiANewArrayInsn", Integer.toString(index++)));
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        addInsn(new MethodInsn(OPCODES[opcode], opcode, owner, name, desc, Integer.toString(index++)));
    }

    public void visitLineNumber(int line, Label start) {
        addInsn(new VisitLineNumberInsn("visitLineNumber", line, start, Integer.toString(index++)));
    }

    public void visitMaxs(int maxStack, int maxLocals) {
        this.maxStack = maxStack;
        this.maxLocals = maxLocals;
        addInsn(new VisitMaxs("visitMaxs", maxStack, maxLocals, Integer.toString(index++)));
    }

    public void visitEnd() {
        addInsn(new VisitEnd("visitEnd", Integer.toString(index++)));
    }
}

