/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
