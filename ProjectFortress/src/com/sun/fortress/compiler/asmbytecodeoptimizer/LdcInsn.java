/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class LdcInsn extends Insn {
    Object cst;

    LdcInsn(String name, Object cst, String index) {
        super(name, index);
        this.cst = cst;
    }

    public String toString() { 
        return "LdcInsn:" + name + " cst = " + cst;
    }
    
    public LdcInsn copy(String newIndex) {
        return new LdcInsn(name, cst, newIndex);
    }

    public void toAsm(MethodVisitor mv) { 
        mv.visitLdcInsn(cst);
    }
}
