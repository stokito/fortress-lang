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

public class VisitLineNumberInsn extends Insn {
    int line;
    Label start;

    VisitLineNumberInsn(String name, int line, Label start, String index) {
        super(name, index);
        this.line = line;
        this.start = start;
    }

    public String toString() { 
        return name + " line = " + line + " start = " + start + " index " + index;
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
