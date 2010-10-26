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
        mv.visitMaxs(Naming.ignoredMaxsParameter,Naming.ignoredMaxsParameter);
    }
}
