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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class Inlining {

    

    public static void Optimize(ByteCodeVisitor bcv) {
        Iterator it = bcv.methodVisitors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            ByteCodeMethodVisitor bcmv = (ByteCodeMethodVisitor) pairs.getValue();
            Inline(bcmv);
        }
    }


    public static boolean isCompilerBuiltin(String name) {
        return name.startsWith("fortress/CompilerBuiltin");
    }


    public static boolean isBuiltinStaticMethod(ByteCodeMethodVisitor bcmv, int i) {
        Insn insn = bcmv.insns.get(i);
        if (insn instanceof MethodInsn) {
            MethodInsn mi = (MethodInsn) insn;
            if ((mi.opcode == Opcodes.INVOKESTATIC) && (isCompilerBuiltin(mi.owner))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBuiltinInterfaceMethod(ByteCodeMethodVisitor bcmv, int i) {
        Insn insn = bcmv.insns.get(i);
        if (insn instanceof MethodInsn) {
            MethodInsn mi = (MethodInsn) insn;
            if ((mi.opcode == Opcodes.INVOKEINTERFACE) && (isCompilerBuiltin(mi.owner))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isStaticMethod(MethodInsn mi, int opcode) {
        return mi.opcode == opcode;
    }

    public static void Inline(ByteCodeMethodVisitor bcmv) {
        ByteCodeOptimizer builtin = new ByteCodeOptimizer();
        builtin.readInJarFile("default_repository/caches/bytecode_cache/fortress.CompilerBuiltin.jar");

        boolean changed = true;
        while (changed == true) {
            changed = false;
            for (int i = 0; i < bcmv.insns.size(); i++) {
                Insn insn = bcmv.insns.get(i);
                if (insn instanceof MethodInsn) {
                    MethodInsn mi = (MethodInsn) insn;
                    if (isCompilerBuiltin(mi.owner)) {
                        ByteCodeVisitor bcv = (ByteCodeVisitor) builtin.classes.get(mi.owner + ".class");
                        ByteCodeMethodVisitor methodToInline = (ByteCodeMethodVisitor) bcv.methodVisitors.get(mi._name + mi.desc);   
                        System.out.println("We have a contender " + mi + " name = " + mi._name + " methodToInline " + methodToInline);
                        System.out.println("replacing " + mi + " with " + methodToInline.insns.size() + " instructions");
                        bcmv.insns.remove(i);
                        for (int j = 0; j < methodToInline.insns.size(); j++) {
                            System.out.println(methodToInline.insns.get(j));
                            bcmv.insns.add(i+j, methodToInline.insns.get(j));
                        }
                        changed = true;
                    }
                }
            }
        }
    }
}