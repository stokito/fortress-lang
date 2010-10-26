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

public class MethodInsn extends Insn {
    int opcode;
    String owner;
    String _name;
    String desc;
    int id;

    static int counter = 0;

    MethodInsn(String name, int opcode, String owner, String _name, String desc, String index) {
        super(name,index);
        this.opcode = opcode;
        this.owner = owner;
        this._name = _name;
        this.desc = desc;
        this.id = counter++;
    }

    public MethodInsn copy(String newIndex) {
        return new MethodInsn(name, opcode, owner, _name, desc, newIndex);
    }

    public String toString() { 
        return "MethodInsn:" + id + " " + index + " " +  opcode + " " + owner + " " + _name + " " + desc;
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitMethodInsn(opcode, owner, _name, desc);
    }

    public boolean matches(int opcode, String owner, String _name, String desc) {
        return ((this.opcode == opcode) &&
                (this.owner.equals(owner)) &&
                (this._name.equals(_name)) &&
                (this.desc.equals(desc)));
    }

    public boolean matches(MethodInsn mi) {
        return matches(mi.opcode, mi.owner, mi._name, mi.desc);
    }

    public void interpret(ByteCodeMethodVisitor bcmv) {
    }
}