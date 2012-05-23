/*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class GenerateUnboxedVersions {
    private final static boolean noisy = false;
    String className;
    ByteCodeMethodVisitor original;
    ByteCodeMethodVisitor generated;
    
    GenerateUnboxedVersions(String className, ByteCodeMethodVisitor bcmv) {
        this.className = className;
        this.original = bcmv;
    }

    public static void optimize(String key, ByteCodeVisitor bcv) {
        Iterator it = bcv.methodVisitors.entrySet().iterator();
        
         while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            ByteCodeMethodVisitor bcmv = (ByteCodeMethodVisitor) pairs.getValue();
            optimizeMethod(key, bcmv);
         }
    }

    public static void optimizeMethod(String key, ByteCodeMethodVisitor bcmv) {
        System.out.println("generateunboxedversions for method " + key + ":" + bcmv.toString());
        Insn i = bcmv.insns.get(0);
        Set<AbstractInterpretationValue> defs = i.getDefs();
        
        for (AbstractInterpretationValue d : defs) {
            String t = d.getType();
            if (t.equals("Lcom/sun/fortress/compiler/runtimeValues/FZZ32;"))  {
                for (Insn u : d.getUses()) {
                    if (u.isUnBoxingMethod())
                        System.out.println("Method " + key + " has parameter " + d + " which is used unboxed by " + u);
                    else 
                        System.out.println("Method " + key + " has parameter " + d + " which is used boxed by " + u);
                }
            }
        }
    }
}
    