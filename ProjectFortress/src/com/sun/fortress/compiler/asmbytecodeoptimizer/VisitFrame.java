/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
        String result = "VisitFrame: type = ";
        switch (type) {
        case Opcodes.F_NEW:      result = result + " F_NEW ";    break;
        case Opcodes.F_FULL:     result = result + " F_FULL ";   break;
        case Opcodes.F_APPEND:   result = result + " F_APPEND "; break;
        case Opcodes.F_CHOP:     result = result + " F_CHOP ";   break;
        case Opcodes.F_SAME:     result = result + " F_SAME ";   break;
        case Opcodes.F_SAME1:    result = result + " F_SAME1 ";  break;
        default: throw new RuntimeException("Unknown Frame Type " + type);
        }

        result = result + "locals = [";
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
        //        System.out.println("About to emit visitFrame : " + toString());
        //        mv.visitFrame(type, nLocal, local, nStack, stack);
    }
}
