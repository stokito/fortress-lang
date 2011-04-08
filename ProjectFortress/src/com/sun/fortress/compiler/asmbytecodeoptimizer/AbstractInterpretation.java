/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import com.sun.fortress.compiler.NamingCzar;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;


public class AbstractInterpretation {
    AbstractInterpretationContext context;

    static List<AbstractInterpretationContext> instructions;
    private final static boolean noisy = false;

    AbstractInterpretation(String className, ByteCodeMethodVisitor bcmv) {
        context = new AbstractInterpretationContext(this, bcmv, 
                                                    new AbstractInterpretationValue[bcmv.maxStack],
                                                    new AbstractInterpretationValue[bcmv.maxLocals],
                                                    0, 0); 
        AbstractInterpretationValue.initializeCount();
        instructions = new ArrayList<AbstractInterpretationContext>();
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
        AbstractInterpretation ai = new AbstractInterpretation(key, bcmv);
        if (noisy) System.out.println("optimize for key = " + key);
        int localsIndex = 0;
        Insn insn = bcmv.insns.get(0);        

        if (!bcmv.isAbstractMethod()) {
            if (!bcmv.isStaticMethod()) {
                String t = "L" + key.replace(".class", ";");
                AbstractInterpretationValue val = bcmv.createValue(insn,t);
                insn.addDef(val);
                ai.context.locals[localsIndex++] = val;
            }
        
            for (int i = 0; i < bcmv.args.size(); i++) {
                String t = bcmv.args.get(i);
                AbstractInterpretationValue val = bcmv.createValue(insn,t);
                insn.addDef(val);
                ai.context.locals[localsIndex+i] = val;
                // Skip the second local, for dual-word values.
                char c = t.charAt(0);
                if (c == 'J' || c == 'D')
                    localsIndex++;
            }

            ai.interpretMethod();
            bcmv.setAbstractInterpretation(ai);
        }
    }

    public void interpretMethod() {
        if (noisy) System.out.println("Interpreting method " + context.bcmv.name + " with access " + context.bcmv.access + " Opcodes.static = " + Opcodes.ACC_STATIC + " bcmv.maxStack = " + context.bcmv.maxStack + " maxLocals = " + context.bcmv.maxLocals);

        context.interpretMethod();
     
        while (!instructions.isEmpty()) {
            context = instructions.remove(0);
            context.interpretMethod();
        }
    }

    public String toString() {
        return context.toString();
    }
}

