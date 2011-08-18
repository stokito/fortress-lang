/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class IincInsn extends Insn {
    int var;
    int increment;

    IincInsn(String name, int var, int increment, String index) {
        super(name, index);
        this.var = var;
        this.increment = increment;
    }

    public IincInsn copy(String newIndex) {
        return new IincInsn(name, var, increment, newIndex);
    }

    public String toString() { 
        return "MethodInsn:" + name + " var = " + var + " inc = " + increment;
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitIincInsn(var,increment);
    }
}
