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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class DefUseChains {

    private static boolean noisy = false;

    public static void optimize(ByteCodeVisitor bcv) {
        Iterator it = bcv.methodVisitors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            ByteCodeMethodVisitor bcmv = (ByteCodeMethodVisitor) pairs.getValue();
            String className = (String) pairs.getKey();
            DefUseChainCalculation(bcmv, className);
        }
    }

    public static void DefUseChainCalculation(ByteCodeMethodVisitor bcmv, String className) {
        System.out.println("DefUseChainCalculation " + bcmv + " for class " + className);
        for (Insn insn : bcmv.insns) {
            if (insn instanceof VarInsn) {
                VarInsn vi = (VarInsn) insn;
                System.out.println("VarInsn " + vi);
            }
        }
    }

    private static void printMethod(ByteCodeMethodVisitor method, String when) {

        if (noisy) {
            System.out.println("Printing Method: " + method.name + "  " + when + " with maxStack = " + method.maxStack + " and maxLocals = " + method.maxLocals);
            printInsns(method.insns, " ");
            System.out.println("End Method: " + method.name);
        }
    }

    private static void printInsns(List<Insn> insns, String header) {

            for (Insn inst : insns) {
                if (inst.isExpanded()) {
                    System.out.println(header + "Replaced Bytecode: " + inst);
                    printInsns(inst.inlineExpansionInsns, header + " ");
                } else {
                    System.out.println(header + "Bytecode: " + inst + inst.getStackString() + inst.getLocals());
                }
            }
    }


    private static void printInsns(List<Insn> insns, int depth) {
        for (Insn inst : insns) {
            if (inst.isExpanded()) {
                System.out.println("(" + depth + ") Replaced Bytecode: " + inst);
                printInsns(inst.inlineExpansionInsns, depth++);
            }
            System.out.println("(" + depth + ") Bytecode: " + inst + inst.getStackString() + inst.getLocals());
        }
    }
}

