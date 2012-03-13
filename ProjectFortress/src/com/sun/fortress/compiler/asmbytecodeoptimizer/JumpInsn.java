/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.*;

public class JumpInsn extends Insn {
    int opcode;
    Label label;

    JumpInsn(String name, int opcode, Label label, String index) {
        super(name, index);
        this.opcode = opcode;
        this.label = label;
    }

    public String toString() { 
        return "JumpInsn:" +  name + ":::" + index + ":::" + " label = " + label ;
    }

    public JumpInsn copy(String newIndex) {
        return new JumpInsn(name, opcode, label, newIndex);
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitJumpInsn(opcode, label);
    }
}
