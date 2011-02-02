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

public class FieldInsn extends Insn {
    int opcode;
    String owner;
    String _name;
    String desc;

    FieldInsn(String name, int opcode, String owner, String _name, String desc, String index) {
        super(name, index);
        this.opcode = opcode;
        this.owner = owner;
        this._name = _name;
        this.desc = desc;
    }

    public String toString() { 
        return "FieldInsn:" + name;
    }

    public FieldInsn copy(String newIndex) {
        return new FieldInsn(name, opcode, owner, _name, desc, newIndex);
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitFieldInsn(opcode, owner, _name, desc);
    }
}
