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