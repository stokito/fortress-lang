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

import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.Pair;
import com.sun.org.apache.bcel.internal.generic.INVOKEINTERFACE;

public class MethodInstantiater implements MethodVisitor {

    MethodVisitor mv;
    InstantiationMap xlation;
    InstantiatingClassloader icl;
    
    public MethodInstantiater(MethodVisitor mv, InstantiationMap xlation, InstantiatingClassloader icl) {
        this.mv = mv;
        this.xlation = xlation;
        this.icl = icl;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return mv.visitAnnotation(xlation.getTypeName(desc), visible);
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
        owner = xlation.getTypeName(owner);
        name = xlation.getTypeName(name);
        desc = xlation.getFieldDesc(desc);
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
        desc = xlation.getFieldDesc(desc);
        signature = xlation.getTypeName(signature);

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
        if (owner.equals(Naming.magicInterpClass)) {
            String oname = name;
            name = xlation.getTypeName(name);
            String op = Naming.encodedOp(name);
            String s = Naming.encodedConst(name);
            if (op.equals(Naming.hashMethod)) {
                long hash_sargs = MagicNumbers.hashStringLong(s);
                mv.visitLdcInsn(Long.valueOf(hash_sargs));
            } else if (op.equals(Naming.stringMethod)) {
                mv.visitLdcInsn(s);
            } else {
                throw new Error("Invocation of magic class Method '"+oname+"' ('"+name+"') seen, but op is not recognized.");
            }
        } else {
            String new_owner = xlation.getTypeName(owner);  // demangled.
            if (opcode == Opcodes.INVOKEINTERFACE && !new_owner.equals(owner) ) {
                if (new_owner.contains(Naming.LEFT_OXFORD)) {
                    if (! new_owner.startsWith("Arrow\u27e6")) {
                        Pair<String, List<Pair<String, String>>> pslpss =
                            icl.xlationForGeneric(new_owner);
                        String stem_sort = pslpss.first();
                        if (stem_sort.equals(Naming.OBJECT_GENERIC_TAG))
                            opcode = Opcodes.INVOKEVIRTUAL;
                        else
                            opcode = opcode; // do nothing
                    } else {
                        opcode = opcode; // do nothing
                    }

                } else {
                    String new_owner_class_name = Naming.mangleFortressIdentifier(new_owner);
                    new_owner_class_name = new_owner_class_name.replaceAll("[/]", ".");
                    try {
                        Class cl = Class.forName(new_owner_class_name, true, icl);
                        if (cl.isInterface()) {
                            // Do nothing
                            opcode = opcode;
                        } else {
                            opcode = Opcodes.INVOKEVIRTUAL;
                        }
                    } catch (ClassNotFoundException e) {
                        // Do nothing, not our problem
                        e = e;
                    }
                }
            }
            name = xlation.getTypeName(name);
            desc = xlation.getMethodDesc(desc);
            mv.visitMethodInsn(opcode, new_owner, name, desc);
        }
    }

    public void visitMultiANewArrayInsn(String desc, int dims) {
        mv.visitMultiANewArrayInsn(desc, dims);
    }

    public AnnotationVisitor visitParameterAnnotation(int parameter,
            String desc, boolean visible) {
        desc = xlation.getTypeName(desc);
        return mv.visitParameterAnnotation(parameter, desc, visible);
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt,
            Label[] labels) {
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler,
            String type) {
        type = xlation.getTypeName(type);
        mv.visitTryCatchBlock(start, end, handler, type);
    }

    public void visitTypeInsn(int opcode, String type) {
        type = xlation.getTypeName(type);
        mv.visitTypeInsn(opcode, type);
    }

    public void visitVarInsn(int opcode, int var) {
        mv.visitVarInsn(opcode, var);
    }
}
