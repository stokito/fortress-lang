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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.util.AbstractVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
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
        return "JumpInsn:" +  name + " label = " + label;
    }

    public JumpInsn copy(String newIndex) {
        return new JumpInsn(name, opcode, label, newIndex);
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitJumpInsn(opcode, label);
    }
}
