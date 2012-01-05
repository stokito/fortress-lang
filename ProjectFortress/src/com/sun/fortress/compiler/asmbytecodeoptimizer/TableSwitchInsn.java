/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class TableSwitchInsn extends Insn {
    int min;
    int max;
    Label dflt;
    Label[] labels;

    TableSwitchInsn(String name, int min, int max, Label dflt, Label[] labels, String index) {
        super(name,index);
        this.min = min;
        this.max = max;
        this.dflt = dflt;
        this.labels = labels;
    }

    public String toString() { 
        return "MethodInsn:" + name + ":(" + min + "," + max + ")";
    }

    public TableSwitchInsn copy(String newIndex) {
        return new TableSwitchInsn(name, min, max, dflt, labels, index);
    }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitTableSwitchInsn(min, max, dflt, labels);
    }
}
