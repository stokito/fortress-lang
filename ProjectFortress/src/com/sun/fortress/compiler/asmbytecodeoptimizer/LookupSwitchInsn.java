/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class LookupSwitchInsn extends Insn {
    Label dflt;
    int[] keys;
    Label[] labels;

    LookupSwitchInsn(String name, Label dflt, int[] keys, Label[] labels, String index) {
        super(name,index);
        this.dflt = dflt;
        this.keys = keys;
        this.labels = labels;
    }

    public String toString() { 
        return "MethodInsn:" + name ;
    }

    public LookupSwitchInsn copy(String newIndex) {
        return new LookupSwitchInsn(name, dflt, keys, labels, newIndex);
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitLookupSwitchInsn(dflt, keys, labels);
    }
}
