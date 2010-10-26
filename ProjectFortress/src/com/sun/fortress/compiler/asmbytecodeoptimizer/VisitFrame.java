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

public class VisitFrame extends Insn {
    int type;
    int nLocal;
    Object local[];
    int nStack;
    Object stack[];

    VisitFrame(int type, int nLocal, Object local[], int nStack, Object stack[], String index) {
        super("VisitFrame", index);
        this.type = type;
        this.nLocal = nLocal;
        this.local = local;
        this.nStack = nStack;
        this.stack = stack;
    }

    public String toString() { 
        String result = "VisitFrame locals = [";
        for (int i = 0; i < nLocal; i++)
            result = result + local[i] + "  ";
        result = result + "] Stack = [";
        for (int i = 0; i < nStack; i++)
            result = result + stack[i] + "  ";
        result = result + "]";
        return result;
    }
    
    public VisitFrame copy(String newIndex) {
        return new VisitFrame(type, nLocal, local, nStack, stack, newIndex);
    }

    public void toAsm(MethodVisitor mv) {
        mv.visitFrame(type, nLocal, local, nStack, stack);
    }
}
