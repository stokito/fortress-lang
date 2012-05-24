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
        return "TypeInsn:" +  name + " with type " + type;
    }

    public TypeInsn copy(String newIndex) {
        return new TypeInsn(name, opcode, type, newIndex);
    }
    
    public boolean isUnnecessaryCheckCast(AbstractInterpretationValue v) {
        return isCheckCast() && type.equals(v.getType());
    }

    public void toAsm(MethodVisitor mv) { 
        mv.visitTypeInsn(opcode, type);
    }

    public String getType() {return type;}

    public boolean isCheckCast() {
        if (opcode == Opcodes.CHECKCAST)
            return true;
        return false;
    }
}
