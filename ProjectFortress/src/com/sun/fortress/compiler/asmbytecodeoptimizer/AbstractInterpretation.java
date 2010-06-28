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
                                       new Object[bcmv.maxStack],
                                       new Object[bcmv.maxLocals],
                                       0, 0); 
        instructions = new ArrayList<AbstractInterpretationContext>();
    }

    public static void optimize(String key, ByteCodeVisitor bcv) {
         Iterator it = bcv.methodVisitors.entrySet().iterator();

         while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            ByteCodeMethodVisitor bcmv = (ByteCodeMethodVisitor) pairs.getValue();
            AbstractInterpretation ai = new AbstractInterpretation(key, bcmv);
            if (noisy) System.out.println("optimize for key = " + key + " pairs.getName() " + pairs.getKey());
            int localsIndex = 0;

            if (!bcmv.isAbstractMethod()) {

                if (!bcmv.isStaticMethod())
                    ai.context.locals[localsIndex++] = "L" + key.replace(".class", ";");
        
                for (int i = 0; i < bcmv.args.size(); i++) {
                    ai.context.locals[localsIndex+i] = bcmv.args.get(i);
                }

                ai.interpretMethod();
            }
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
}