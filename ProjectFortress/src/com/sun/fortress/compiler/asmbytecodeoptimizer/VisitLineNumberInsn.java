/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class VisitLineNumberInsn extends Insn {
    int line;
    Label start;

    VisitLineNumberInsn(String name, int line, Label start, String index) {
        super(name, index);
        this.line = line;
        this.start = start;
    }

    public String toString() { 
        return name + " line = " + line + " start = " + start;
    }
    
    public void toAsm(MethodVisitor mv) {
        mv.visitLineNumber(line, start);
    }

    public VisitLineNumberInsn copy(String newIndex) {
        return new VisitLineNumberInsn(name, line, start, newIndex);
    }
        

    public boolean matches(VisitLineNumberInsn vlni) {
        return true;
    }
}
