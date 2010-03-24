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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

/* We are looking for two instructions in a row.  FIntLiteral.make() followed by coerce_ZZ32().  If the number on the stack is small enough we can replace these two function calls by a single call to ZZ32.make(); */

public class RemoveLiteralCoercions {

    static String intLiteral = "com/sun/fortress/compiler/runtimeValues/FIntLiteral";
    static String ZZ32 = "com/sun/fortress/compiler/runtimeValues/FZZ32";
    static String coerce = "fortress/CompilerBuiltin.coerce_ZZ32";
    

    public static void Optimize(ByteCodeVisitor bcv) {
        Iterator it = bcv.methodVisitors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            ByteCodeMethodVisitor bcmv = (ByteCodeMethodVisitor) pairs.getValue();
            removeCoercions(bcmv);
        }
    }
    public static void removeCoercions(ByteCodeMethodVisitor bcmv) {
        for (int i = 0; i < bcmv.insns.size(); i++) {
            if (bcmv.insns.get(i) instanceof MethodInsn) {
                MethodInsn mi = (MethodInsn) bcmv.insns.get(i);
                if (mi.matches(bcmv.INVOKESTATIC, intLiteral, "make", "(I)L" + intLiteral + ";"))  {
                    // i+1 is the label, i+2 is visitLineNumber, i+3 the next instruction
                    if (bcmv.insns.get(i+3) instanceof MethodInsn) {
                        MethodInsn mi2 = (MethodInsn) bcmv.insns.get(i+3);
                        if (mi2.matches(bcmv.INVOKESTATIC, "fortress/CompilerBuiltin", "coerce_ZZ32",
                                        "(L" + intLiteral + ";)L" + ZZ32 + ";")) {
                            bcmv.insns.remove(i);
                            bcmv.insns.remove(i);
                            bcmv.insns.remove(i);
                            bcmv.insns.remove(i);
                            bcmv.insns.add(i, new MethodInsn("INVOKESTATIC", bcmv.INVOKESTATIC, 
                                                             ZZ32, 
                                                             "make",
                                                             "(I)L" + ZZ32 + ";"));
                        }
                    }
                }
            }
        }
    }
}
