/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;
import java.util.Iterator;
import java.util.Map;


public class AbstractInterpretation {
    private final static boolean noisy = false;
    String className;
    ByteCodeMethodVisitor bcmv;
    AbstractInterpretationContext context;

    AbstractInterpretation(String className, ByteCodeMethodVisitor bcmv) {
        this.className = className;
        this.bcmv = bcmv;
        this.context = new AbstractInterpretationContext(bcmv,
                                                         new AbstractInterpretationValue[bcmv.maxStack],
                                                         new AbstractInterpretationValue[bcmv.maxLocals],
                                                         0, 0);
        AbstractInterpretationValue.initializeCount();
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
        int localsIndex = 0;
        Insn insn = bcmv.insns.get(0);        

        if (!bcmv.isAbstractMethod()) {
            if (!bcmv.isStaticMethod()) {
                String t = "L" + key.replace(".class", ";");
                AbstractInterpretationValue val = bcmv.createValue(insn,t);
                insn.addDef(val);
                localsIndex = ai.context.setLocal(localsIndex, val);
            }
        
            for (int i = 0; i < bcmv.args.size(); i++) {
                String t = bcmv.args.get(i);
                AbstractInterpretationValue val = bcmv.createValue(insn,t);
                insn.addDef(val);
                localsIndex = ai.context.setLocal(localsIndex, val);
            }
            ai.interpretMethod();
                
            bcmv.setAbstractInterpretation(ai);
        }
    }

    public void interpretInsns(AbstractInterpretationContext c, int start) {
        c.pc = start;
         while (c.pc < bcmv.insns.size()) {
             AbstractInterpretationInsn.interpretInsn(c, bcmv.insns.get(c.pc), c.pc);
             c.pc++;
         }
    }        

    public void interpretMethod() {
        long startTime = 0;

        if (noisy) {
            System.out.println("Interpreting method " + bcmv.name + " with access " + bcmv.access + " Opcodes.static = " + Opcodes.ACC_STATIC + " bcmv.maxStack = " + bcmv.maxStack + " maxLocals = " + bcmv.maxLocals);
        }

        interpretInsns(context, 0);

        while (!context.branchTargets.isEmpty()) {
            AbstractInterpretationContext c = context.branchTargets.remove(0);
            interpretInsns(c,c.pc);
        }
            

         for (AbstractInterpretationContext c : context.branchTargets) {
             interpretInsns(c, c.pc);
         }

        if (noisy) {
            System.out.println("Finished method " + bcmv.name + " in " + (System.currentTimeMillis() - startTime) + " ms");
        }

    }


   
    public String getStackString() {
        String result = "[";
        for(int i = 0; i < context.stackIndex; i++) 
            result = result + " " + context.stack[i];
        result = result + "]";
        return result;
    }

    public String getLocalsString() {
        String result = "{";
        for (int i=0; i < context.locals.length; i++)
            result = result + " " + context.locals[i];
        result = result + "}";
        return result;
    }


    void printInsn(Insn i, int pc) {
        if (noisy)  {
            System.out.println("InterpretInsn: pc= " + pc + " insn = " + i + getStackString() + getLocalsString());
        }
    }        


    public String toString() {
        return  getStackString() + getLocalsString();
    }
}

