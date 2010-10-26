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

public class LocalVariableInsn extends Insn {
    String name;
    String _name;
    String desc;
    String sig;
    Label start;
    Label end;
    int _index;

    LocalVariableInsn(String name, String _name, String desc, String sig, Label start, Label end, int _index, String index) {
        super(name, index);
        this.name = name;
        this._name = _name;
        this.desc = desc;
        this.sig = sig;
        this.start = start;
        this.end = end;
        this._index = _index;
    }

    public LocalVariableInsn copy(String newIndex) {
        return new LocalVariableInsn(name, _name, desc, sig, start, end, _index, newIndex);
    }

    public String toString() { 
        return "LocalVariableInsn" +  _name;
    }

    public void toAsm(MethodVisitor mv) { 
        mv.visitLocalVariable(_name, desc, sig, start, end, _index);
    }
}
