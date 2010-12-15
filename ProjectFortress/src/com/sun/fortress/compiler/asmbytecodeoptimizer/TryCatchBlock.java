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
