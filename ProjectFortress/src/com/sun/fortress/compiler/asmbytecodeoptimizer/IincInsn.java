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

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class IincInsn extends Insn {
    int var;
    int increment;

    IincInsn(String name, int var, int increment) {
        this.name = name;
        this.var = var;
        this.increment = increment;
    }

    public String toString() { 
        return "MethodInsn:" + name + " var = " + var + " inc = " + increment;
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitIincInsn(var,increment);
    }
}