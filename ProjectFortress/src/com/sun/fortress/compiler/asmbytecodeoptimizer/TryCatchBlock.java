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

public class TryCatchBlock extends Insn {
    Label start;
    Label end;
    Label handler;
    String type;

    TryCatchBlock(String name, Label start, Label end, Label handler, String type, String index){
        super(name, index);
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.type = type;
    }

    public String toString() { 
        return "TryCatchBlock:::" + index + ":::" + " start = " + start + " end = " + end + " handler = " + handler + " type = " + type ;
    }

    public TryCatchBlock copy(String newIndex) {
        return new TryCatchBlock(name, start, end, handler, type, newIndex);
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitTryCatchBlock(start, end, handler, type);
    }
}
