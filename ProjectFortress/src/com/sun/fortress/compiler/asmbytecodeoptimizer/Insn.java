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

    public String toString() { return name; }
    public void setStack(Object stack[]) {this.stack = stack;}
    public void setLocals(Object locals[]) {this.locals = locals;}

    public abstract void toAsm(MethodVisitor mv);

    public boolean matches(Insn i) {
        return false;
    }
}