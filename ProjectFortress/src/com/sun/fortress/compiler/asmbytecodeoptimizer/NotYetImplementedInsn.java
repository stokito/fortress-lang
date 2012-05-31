/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class NotYetImplementedInsn extends Insn {

    NotYetImplementedInsn(String name, String index) {
        super(name, index);
        throw new RuntimeException("Encountered a not yet implemented instruction: " + name);
    }

    public NotYetImplementedInsn copy(String newIndex) {
        return new NotYetImplementedInsn(name, newIndex);
    }

    public String toString() { 
        return "Not Yet Implemented " + name;
    }
    
    public void toAsm(MethodVisitor mv) {
        throw new RuntimeException("Encountered a NYI instruction: " + name);
    }
}
