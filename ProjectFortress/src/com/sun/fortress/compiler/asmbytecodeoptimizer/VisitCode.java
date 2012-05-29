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


public class VisitCode extends Insn {

    VisitCode(String index) {
        super("VisitCode", index);
    }

    public String toString() {
        return "VisitCode";
    }

    public VisitCode copy(String newIndex) {
        return new VisitCode(index);
    }

    public boolean matches(Insn i) { 
        if (i instanceof VisitCode) {
            System.out.println("We matched" + i);
            return true; 
        } else {
            System.out.println("We didn't match " + i);
            return false;
        }
    }

    public void toAsm(MethodVisitor mv) {
        mv.visitCode();
    }
}
