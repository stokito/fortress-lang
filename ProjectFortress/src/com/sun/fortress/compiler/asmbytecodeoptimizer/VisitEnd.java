/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class VisitEnd extends Insn {

    VisitEnd(String name, String index) {
        super(name,index);
    }

    public String toString() { 
        return name;
    }

    public VisitEnd copy(String newIndex) {
        return new VisitEnd(name, newIndex);
    }
    
    public void toAsm(MethodVisitor mv) {
        mv.visitEnd();
    }
}
