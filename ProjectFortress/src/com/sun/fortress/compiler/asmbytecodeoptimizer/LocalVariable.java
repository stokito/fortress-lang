/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class LocalVariable extends Insn {
    String _name;
    String desc;
    String sig;
    Label start;
    Label end;
    int _index;

    LocalVariable(String name, String _name, String desc, String sig, Label start, Label end, int _index, String index) {
        super(name, index);
        this._name = _name;
        this.desc = desc;
        this.sig = sig;
        this.start = start;
        this.end = end;
        this._index = _index;
    }

    public String toString() { 
        return "LocalVariable" +  _name;
    }

    public LocalVariable copy(String newIndex) {
        return new LocalVariable(name, _name, desc, sig, start, end, _index, newIndex);
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitLocalVariable(_name, desc, sig, start, end, _index);
    }
}
