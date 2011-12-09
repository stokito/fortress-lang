/*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
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

class AbstractInterpretationContext {
    ByteCodeMethodVisitor bcmv;
    AbstractInterpretationValue[] stack;
    AbstractInterpretationValue[] locals;
    int stackIndex;
    int pc;
    Insn cause;
    static boolean noisy = false;
    static List<AbstractInterpretationContext> branchTargets = new ArrayList<AbstractInterpretationContext>();

    AbstractInterpretationContext(ByteCodeMethodVisitor bcmv, 
                                  AbstractInterpretationValue[] stack, 
                                  AbstractInterpretationValue[] locals,
                                  int stackIndex,
                                  int pc) {
        this.bcmv = bcmv;
        this.stack = stack;
        this.locals = locals;
        this.stackIndex = stackIndex;
        this.pc = pc;
        this.cause = bcmv.insns.get(0);
    }    

    AbstractInterpretationContext(Insn cause, AbstractInterpretationContext c, int pc) {
        this.bcmv = c.bcmv;
        this.stack = c.stack;
        this.locals = c.locals;
        this.stackIndex = c.stackIndex;
        this.pc = pc;
        this.cause = cause;
        if (noisy)
            System.out.println("Creating an AIC caused by " + cause + " with pc = " + pc);
    }

    String getStackString() {
        String result = pc + "[";
        int i = 0;
        for (; i < stackIndex-1; i++)
            result = result + stack[i] + ",";
        if (i == stackIndex - 1)
            result = result + stack[stackIndex - 1];
        return result + "]" + cause;
    }

    static void addBranchTarget(AbstractInterpretationContext c) {
        branchTargets.add(c);
    }

    boolean isDoubleWideType(AbstractInterpretationValue v) {
        return v.getType().equals("J") || v.getType().equals("D");
    }

    Integer getJumpDestination(String s) { 
        return (Integer) bcmv.labelDefs.get(s);}

    int getNext(JumpInsn i) {
        Integer loc = getJumpDestination(i.label.toString());
        if (loc != null) return loc.intValue();
        else return 0;
    }

    void finished() {
        pc = bcmv.insns.size();
    }
            
    int setLocal(int index, AbstractInterpretationValue val) {
        int i = index;
        locals[i++] = val;
        if (isDoubleWideType(val)) {
            locals[i++] = bcmv.createSecondSlotValue(val);
        }
        return i;
    }

    AbstractInterpretationValue getLocal(int index) {
        AbstractInterpretationValue res = locals[index];
        if (isDoubleWideType(res)) {
            AbstractInterpretationSecondSlotValue other = (AbstractInterpretationSecondSlotValue) locals[index+1];
        }
        return res;
    }

    void pushStackDefinition(Insn i, String desc) {
        AbstractInterpretationValue s = bcmv.createValue(i, desc);
        if (noisy) {
            System.out.print(getStackString());
            System.out.println("pushStackDefinition: Insn = " + i + " desc = " + desc + " value = " + s);
        }
        i.addDef(s);
        s.addDefinition(i);
        if (isDoubleWideType(s)) {
            AbstractInterpretationSecondSlotValue ss = bcmv.createSecondSlotValue(s);
            stack[stackIndex++] = ss;
            i.addDef(ss);
            ss.addDefinition(i);
        }
        stack[stackIndex++] = s;
        i.setStack(stack);

    }

    void pushStackBoxedDefinition(Insn i, String result, AbstractInterpretationValue val) {
        if (noisy) System.out.print(getStackString());        
        AbstractInterpretationBoxedValue bv = bcmv.createBoxedValue(i, result, val);
        if (noisy)
            System.out.println("pushStackBoxedDefinition: Insn = " + i + 
                               " result = " + result + " value = " + val);
        i.addDef(bv);
        bv.addDefinition(i);
        stack[stackIndex++] = bv;
        i.setStack(stack);
    }
    
    void pushStack(Insn i, AbstractInterpretationValue s) {
        if (noisy) {
            System.out.print(getStackString());        
            System.out.println("pushStack: Insn = " + i + " value = " + s); 
        }
        if (stackIndex >= stack.length) {
            System.out.println("pushStack overflow: StackIndex = " + stackIndex);
            System.out.println("BCMV = " + bcmv + " Insn = " + i);
            for (int j = 0; j < stackIndex; j++)
                System.out.println("Stack Item: " + j + " = " + stack[j]);
            int count = 0;
            for (Insn insn : bcmv.insns) {
                if (i.matches(insn)) System.out.print("****>");
                System.out.println("Insn " + count++ + " = " + insn + " stack = " + insn.getStackString());
            }
            throw new RuntimeException("pushStack: i = " + i + " causes stack overflow");
        }
        s.addUse(i);
        i.addUse(s);
        if (isDoubleWideType(s)) {
                AbstractInterpretationSecondSlotValue ss = bcmv.createSecondSlotValue(s);
                stack[stackIndex++] = ss;
                i.addUse(ss);
                ss.addUse(i);
            }
        stack[stackIndex++] = s;
        i.setStack(stack);

    }

    AbstractInterpretationValue popStack(Insn i) { 
        if (noisy) System.out.print(getStackString());        
        AbstractInterpretationValue result = stack[--stackIndex];
        if (noisy) System.out.println("popStack: Insn = " + i + " result = " + result);
        if (isDoubleWideType(result)) {
            AbstractInterpretationSecondSlotValue other = 
                (AbstractInterpretationSecondSlotValue) stack[--stackIndex];
            i.addUse(other);
            other.addUse(i);
        }
        i.addUse(result);
        result.addUse(i);
        i.setStack(stack);
        return result;
    }

}
