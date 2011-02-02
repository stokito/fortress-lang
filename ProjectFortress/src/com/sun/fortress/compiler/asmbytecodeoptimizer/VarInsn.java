/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class VarInsn extends Insn {
    int opcode;
    int var;

    VarInsn(String name, int opcode, int var, String index) {
        super(name, index);
        this.opcode = opcode;
        this.var = var;
    }
    public String toString() { 
        return "VarInsn:" +  name + " variable = " + var;
    }
    
    public VarInsn copy(String newIndex) {
        return new VarInsn(name, opcode, var, newIndex);
    }

    public void toAsm(MethodVisitor mv) { 
        mv.visitVarInsn(opcode, var);
    }
}
