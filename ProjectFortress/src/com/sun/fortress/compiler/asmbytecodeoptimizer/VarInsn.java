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
    String addedBy;

    VarInsn(String name, int opcode, int var, String index) {
        super(name, index);
        this.opcode = opcode;
        this.var = var;
    }

    VarInsn(String name, int opcode, int var, String index, String addedBy) {
        super(name, index);
        this.opcode = opcode;
        this.var = var;
        this.addedBy = addedBy;
    }

    public boolean matches(VarInsn vi) {
        boolean result = (opcode == vi.opcode) && (var == vi.var);
        return result;
    }

    public String toString() { 
        return "VarInsn:" +  name + " opcode = " + opcode + " index = " + index + " newIndex = " + newIndex + " variable = " + var;
    }
    
    public VarInsn copy(String newIndex) {
        return new VarInsn(name, opcode, var, newIndex);
    }

    public void toAsm(MethodVisitor mv) { 
        mv.visitVarInsn(opcode, var);
    }
}
