/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class IntInsn extends Insn {
    String _name;
    int opcode;
    int operand;

    IntInsn(String name, int opcode, int operand, String index) {
        super("IntInsn", index);
        this._name = name;
        this.opcode = opcode;
        this.operand = operand;
    }
    public String toString() { 
        return "MethodInsn:" +  name + " " + operand;
    }

    public IntInsn copy(String newIndex) {
        return new IntInsn(name, opcode, operand, newIndex);
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitIntInsn(opcode, operand);
    }
}
