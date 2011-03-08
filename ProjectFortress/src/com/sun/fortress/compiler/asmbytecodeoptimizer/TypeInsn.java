/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class TypeInsn extends Insn {
    int opcode;
    String type;

    TypeInsn(String name, int opcode, String type, String index) {
        super(name, index);
        this.opcode = opcode;
        this.type = type;
    }
    public String toString() { 
        return "TypeInsn:" +  name;
    }

    public TypeInsn copy(String newIndex) {
        return new TypeInsn(name, opcode, type, newIndex);
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitTypeInsn(opcode, type);
    }

    public boolean isCheckCast() {
        if (opcode == Opcodes.CHECKCAST)
            return true;
        return false;
    }
}
