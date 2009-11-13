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
package com.sun.fortress.runtimeSystem;

import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class MethodInstantiater implements MethodVisitor {

    MethodVisitor mv;
    Instantiater.BetterInstantiationMap xlation;
    
    public MethodInstantiater(MethodVisitor mv, Instantiater.BetterInstantiationMap xlation) {
        this.mv = mv;
        this.xlation = xlation;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return mv.visitAnnotation(xlation.getCompletely(desc), visible);
    }

    public AnnotationVisitor visitAnnotationDefault() {
        return mv.visitAnnotationDefault();
    }

    public void visitAttribute(Attribute attr) {
        // Let's hope we don't need to translate these
        mv.visitAttribute(attr);
    }

    public void visitCode() {
        mv.visitCode();
    }

    public void visitEnd() {
        mv.visitEnd();
    }

    public void visitFieldInsn(int opcode, String owner, String name,
            String desc) {
        owner = xlation.getCompletely(owner);
        name = xlation.getCompletely(name);
        desc = xlation.getDesc(desc);
        mv.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitFrame(int type, int nLocal, Object[] local, int nStack,
            Object[] stack) {
        mv.visitFrame(type, nLocal, local, nStack, stack);
    }

    public void visitIincInsn(int var, int increment) {
        mv.visitIincInsn(var, increment);
    }

    public void visitInsn(int opcode) {
        mv.visitInsn(opcode);
    }

    public void visitIntInsn(int opcode, int operand) {
        mv.visitIntInsn(opcode, operand);
    }

    public void visitJumpInsn(int opcode, Label label) {
        mv.visitJumpInsn(opcode, label);
    }

    public void visitLabel(Label label) {
        mv.visitLabel(label);
    }

    public void visitLdcInsn(Object cst) {
        mv.visitLdcInsn(cst);
    }

    public void visitLineNumber(int line, Label start) {
        mv.visitLineNumber(line, start);
    }

    public void visitLocalVariable(String name, String desc, String signature,
            Label start, Label end, int index) {
        desc = xlation.getCompletely(desc);
        signature = xlation.getCompletely(signature);

        mv.visitLocalVariable(name, desc, signature, start, end, index);
    }

    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        mv.visitLookupSwitchInsn(dflt, keys, labels);
    }

    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack, maxLocals);
    }

    public void visitMethodInsn(int opcode, String owner, String name,
            String desc) {
        owner = xlation.getCompletely(owner);
        name = xlation.getCompletely(name);
        desc = xlation.getDesc(desc);
        mv.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
        mv.visitMultiANewArrayInsn(desc, dims);
    }

    public AnnotationVisitor visitParameterAnnotation(int parameter,
            String desc, boolean visible) {
        desc = xlation.getCompletely(desc);
        return mv.visitParameterAnnotation(parameter, desc, visible);
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt,
            Label[] labels) {
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler,
            String type) {
        type = xlation.getCompletely(type);
        mv.visitTryCatchBlock(start, end, handler, type);
    }

    public void visitTypeInsn(int opcode, String type) {
        type = xlation.getCompletely(type);
        mv.visitTypeInsn(opcode, type);
    }

    public void visitVarInsn(int opcode, int var) {
        mv.visitVarInsn(opcode, var);
    }
}
