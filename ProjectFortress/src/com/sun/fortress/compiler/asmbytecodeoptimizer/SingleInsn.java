/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class SingleInsn extends Insn {
    String op;
    int opcode;

    SingleInsn(String op, int opcode, String index) {
        super("SingleInsn:" + op , index);
        this.op = op;
        this.opcode = opcode;
    }
    public String toString() { 
        return "SingleInsn:" + name;
    }

    public SingleInsn copy(String newIndex) {
        return new SingleInsn(op, opcode, newIndex);
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitInsn(opcode);
    }

    public boolean isAReturn() {
        if (opcode == Opcodes.ARETURN)
            return true;
        return false;
    }

}
