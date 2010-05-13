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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import java.util.Iterator;
import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;


// We take a list of Insns and break them into Basic Blocks.

public class BasicBlockGenerator {

    public static void optimize(ByteCodeVisitor bcv) {
        Iterator it = bcv.methodVisitors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            ByteCodeMethodVisitor bcmv = (ByteCodeMethodVisitor) pairs.getValue();
            makeBasicBlocks(bcmv);
            printBasicBlocks(bcmv);
        }
    }

    public static void markBasicBlockStarts(ByteCodeMethodVisitor bcmv) {
        bcmv.insns.get(0).markStartOfBasicBlock();
        for (int i = 1; i < bcmv.insns.size(); i++) {
            if (bcmv.insns.get(i-1) instanceof JumpInsn) {
                JumpInsn ji = (JumpInsn) bcmv.insns.get(i-1);
                Label l = ji.label;
                bcmv.insns.get(i).markStartOfBasicBlock();
                Integer destination = (Integer) bcmv.labelNames.get(l.toString());
                bcmv.insns.get(destination.intValue()).markStartOfBasicBlock();
                bcmv.insns.get(i-1).addNext(destination);
                bcmv.insns.get(i-1).addNext(new Integer(i));
            } else if ((bcmv.insns.get(i-1) instanceof LookupSwitchInsn) ||
                (bcmv.insns.get(i-1) instanceof TableSwitchInsn))
                throw new RuntimeException("NYI: Switch Insns are not implemented yet");
        }
    }
        
    public static void markBasicBlockEnds(ByteCodeMethodVisitor bcmv) {
        for (int i = 1; i < bcmv.insns.size(); i++) {
            if (bcmv.insns.get(i).isStartOfBasicBlock()) {
                bcmv.insns.get(i-1).markEndOfBasicBlock();
            }
        }
        bcmv.insns.get(bcmv.insns.size() -1).markEndOfBasicBlock();
    }

    public static void makeBasicBlocks(ByteCodeMethodVisitor bcmv) {
        markBasicBlockStarts(bcmv);
        markBasicBlockEnds(bcmv);
    }
            
    public static void printBasicBlocks(ByteCodeMethodVisitor bcmv) {
        System.out.println("Printing Basic Blocks for : " + bcmv.name);
        for (int i =0; i < bcmv.insns.size(); i++) {        
            System.out.print(bcmv.insns.get(i));
            if (bcmv.insns.get(i).isStartOfBasicBlock())
                System.out.print("   is start of Basic block");
            if (bcmv.insns.get(i).isEndOfBasicBlock())
                System.out.print("    is end of Basic Block with continuations " + bcmv.insns.get(i).NextInsns);
            System.out.println();
        }
        System.out.println("End of Basic Blocks for : " + bcmv.name);
    }
}