/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.compiler.asmbytecodeoptimizer;


import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract public class Insn {
    protected String name;
    protected AbstractInterpretationValue stack[];
    protected AbstractInterpretationValue locals[];

    protected Set<AbstractInterpretationValue> uses;
    protected Set<AbstractInterpretationValue> defs;
    // Imagine a function foo which inlines bar which inlines baz which has a label and a goto
    // The indices are after the instruction in parenthesis.
    // foo(1)
    //    bar(1.1)
    //       baz (1.1.1)
    //          label start (1.1.1.1)
    //          goto start (1.1.1.2)
    //    bar(1.2)
    //       baz (1.2.1)
    //          label start (1.2.1.1)
    //          goto start (1.1.1.2)
    // end
    // Our last step after inlining is to rename labels to avoid the duplicate start
    // label problem above.  When we are generating the ASM for labels, we change
    // their 

    protected String index;

    public String getIndex() { return index;}
    public void setIndex(String s) {index = s;}

    // newIndex is the new index of this instruction after inlining.
    protected int newIndex;
    public int getNewIndex() { return newIndex;}
    public void setNewIndex(int i) { newIndex = i;}

    protected ArrayList<Insn> inlineExpansionInsns = new ArrayList<Insn>();
    protected Insn parentInsn;

    public void setParentInsn(Insn p) {parentInsn = p;}
    public Insn getParentInsn() {return parentInsn;}
    

    Insn(String name, String index) {
        this.name = name;
        this.index = index;
        this.newIndex = 0;
        this.uses = new HashSet<AbstractInterpretationValue>();
        this.defs = new HashSet<AbstractInterpretationValue>();
    }

    public String toString() { 
        if (isExpanded())
            return "Expanded" + " ind = " + index + " " + name;
        else return name + " ind = " + index; 
    }

    public void setStack(AbstractInterpretationValue stack[]) {
        this.stack = new AbstractInterpretationValue[stack.length];
        for (int i = 0; i < stack.length; i++) {
            this.stack[i] = stack[i];
        }
    }

    public void setLocals(AbstractInterpretationValue locals[]) {
        this.locals = new AbstractInterpretationValue[locals.length];
        for (int i = 0; i < locals.length; i++) {
            this.locals[i] = locals[i];
        }
    }
        

    public abstract void toAsm(MethodVisitor mv);

    public abstract Insn copy(String index);

    public void toAsmWrapper(MethodVisitor mv) {
        if (isExpanded()) {
             for (int i = 0; i < inlineExpansionInsns.size(); i++)
                 inlineExpansionInsns.get(i).toAsmWrapper(mv);
         } else {
             toAsm(mv);
         }
     }

    public boolean matches(Insn i) {
        return false;
    }

    public boolean isExpanded() {
        return inlineExpansionInsns.size() > 0 ;
    }

    public void assignIndex(String i) {index = i;}

    public String getStackString() {
        String result = "[";
        if (stack != null) {
            for (Object o : stack)
                result = result + o + " ";
        }
        result = result + "]";
        return result;
    }

    public String getLocals() {
        String result = "{";
        if (locals != null) {
            for (AbstractInterpretationValue l : locals)
                result = result + l + " ";
        }
        result = result +"}";
        return result;
    }

    public boolean isCheckCast() {
        if (this instanceof TypeInsn) {
            TypeInsn ti = (TypeInsn) this;
            return ti.isCheckCast();
        }
        return false;
    }

    public boolean isUnnecessaryCheckCast(AbstractInterpretationValue v) {
        return false;
    }

    public boolean isBoxingMethod() {
        if (this instanceof MethodInsn) {
            MethodInsn mi = (MethodInsn) this;
            return mi.isBoxingMethod();
        }
        return false;
    }

    public boolean isUnBoxingMethod() {
        if (this instanceof MethodInsn) {
            MethodInsn mi = (MethodInsn) this;
            return mi.isUnBoxingMethod();
        }
        return false;
    }

    public boolean isAReturn() {
        if (this instanceof SingleInsn) {
            SingleInsn si = (SingleInsn) this;
            return si.isAReturn();
        }
        return false;
    }

    public void addUse(AbstractInterpretationValue v) {
        uses.add(v);
    }

    public Set<AbstractInterpretationValue> getUses() {
        return uses;
    }

    public boolean hasMultipleUses() {
        if (uses.size() > 1)
            return true;
        else return false;
    }

    public void addDef(AbstractInterpretationValue v) {
        if (!defs.contains(v)) defs.add(v);
    }

    public boolean hasDef() {
        if (defs.isEmpty())
            return false;
        else return true;
    }

    public Set<AbstractInterpretationValue> getDefs() {
        return defs;
    }

}

