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

public class LabelInsn extends Insn {
    Label label;

    LabelInsn(String name , Label label, String index) {
        super(name, index);
        this.label = label;
    }

    public LabelInsn copy(String newIndex) {
        return new LabelInsn(name, label, newIndex);
    }

    public String toString() { 
        return "LabelInsn:" + name + " " + label + ":::" + index;
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitLabel(label);
    }

    public boolean matches(LabelInsn li) {
        return true;
    }
}
