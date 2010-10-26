/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
******************************************************************************/

package com.sun.fortress.compiler.asmbytecodeoptimizer;


import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

abstract public class Insn {
    String name;
    Object locals[];
    Object stack[];

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

    // Then when we rename label start it will be either start.1.1.1.1 or start.1.2.1.1 and 
    // this saves us from the duplicate label problem.
    String index;

    ArrayList<Insn> inlineExpansionInsns = new ArrayList<Insn>();
    Insn parentInsn;

    Insn(String name, String index) {
        this.name = name;
        this.index = index;
    }

    public String toString() { 
        if (isExpanded())
            return "Expanded" + " ind = " + index + " " + name;
        else return name + " ind = " + index; 
    }

    public void setStack(Object stack[]) {this.stack = stack;}
    public void setLocals(Object locals[]) {this.locals = locals;}

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
            for (Object o : locals)
                result = result + o + " ";
        }
        result = result +"}";
        return result;
    }
}