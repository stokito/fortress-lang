/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import com.sun.fortress.runtimeSystem.Naming;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;



public class VisitMaxs extends Insn {
    int maxStack;
    int maxLocals;

    VisitMaxs(String name, int maxStack, int maxLocals, String index) {
        super(name, index);
        this.maxStack = maxStack;
        this.maxLocals = maxLocals;
    }

    public String toString() { 
        return name + " maxStack = " + maxStack + " maxLocals = " + maxLocals;
    }

    public VisitMaxs copy(String newIndex) {
        return new VisitMaxs(name, maxStack, maxLocals, newIndex);
    }
    
    public void toAsm(MethodVisitor mv) {
        mv.visitMaxs(0,0);
        //        mv.visitMaxs(1024,1024);
        //        mv.visitMaxs(maxStack, maxLocals);
    }
}
