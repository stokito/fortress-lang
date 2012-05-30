/*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;
import java.util.Iterator;
import java.util.Map;

public class CalculateLabels {

    public static void optimize(ByteCodeVisitor bcv) {
        Iterator it = bcv.methodVisitors.entrySet().iterator();
        
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();            
            ByteCodeMethodVisitor bcmv = (ByteCodeMethodVisitor) pairs.getValue();
            optimizeMethod(bcmv);
        }
    }


    public static void optimizeMethod(ByteCodeMethodVisitor bcmv) {
        for (int i = 0 ; i < bcmv.insns.size(); i++) {
            Insn insn = bcmv.insns.get(i);
            if (insn instanceof LabelInsn) {
                LabelInsn li = (LabelInsn) insn;
                bcmv.labelDefs.put(li.label.toString(), i);
            }
        }
    }
}
