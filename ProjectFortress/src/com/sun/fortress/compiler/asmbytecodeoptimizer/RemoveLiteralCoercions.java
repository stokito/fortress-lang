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

public class RemoveLiteralCoercions {

    public static void optimize(ByteCodeVisitor bcv) {
        Iterator it = bcv.methodVisitors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            ByteCodeMethodVisitor bcmv = (ByteCodeMethodVisitor) pairs.getValue();
            removeCoercions(bcmv);
        }
    }

    public static Substitution removeIntLiterals(ByteCodeMethodVisitor bcmv) {
        String intLiteral = "com/sun/fortress/compiler/runtimeValues/FIntLiteral";
        String ZZ32 = "com/sun/fortress/compiler/runtimeValues/FZZ32";

        ArrayList<Insn> matches = new ArrayList<Insn>();
        matches.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC,
                                             intLiteral, 
                                             "make",
                                             "(I)L" + intLiteral + ";"));
        matches.add(new LabelInsn("LabelInsn", new Label()));
        matches.add(new VisitLineNumberInsn("visitlinenumber", 0, new Label()));
        matches.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, "fortress/CompilerBuiltin", 
                                             "coerce_ZZ32", 
                                             "(Lfortress/CompilerBuiltin$IntLiteral;)L" + ZZ32 + ";"));
        ArrayList<Insn> replacements = new ArrayList<Insn>();
        replacements.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, 
                                            ZZ32,
                                            "make",
                                            "(I)L" + ZZ32 + ";"));
        return new Substitution(matches, replacements);
    }

    public static Substitution removeFloatLiterals(ByteCodeMethodVisitor bcmv) {
        String floatLiteral = "com/sun/fortress/compiler/runtimeValues/FFloatLiteral";
        String RR64 = "com/sun/fortress/compiler/runtimeValues/FRR64";

        ArrayList<Insn> matches = new ArrayList<Insn>();
        matches.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC,
                                             floatLiteral, 
                                             "make",
                                             "(D)L" + floatLiteral + ";"));
        matches.add(new LabelInsn("LabelInsn", new Label()));
        matches.add(new VisitLineNumberInsn("visitlinenumber", 0, new Label()));
        matches.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, "fortress/CompilerBuiltin", 
                                             "coerce_RR64", 
                                             "(Lfortress/CompilerBuiltin$FloatLiteral;)L" + RR64 + ";"));
        ArrayList<Insn> replacements = new ArrayList<Insn>();
        replacements.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, 
                                            RR64,
                                            "make",
                                            "(D)L" + RR64 + ";"));
        return new Substitution(matches, replacements);
    }


    public static void removeCoercions(ByteCodeMethodVisitor bcmv) {
        //        for (int i = 0; i < bcmv.insns.size(); i++) {
        //            System.out.println("Insn " + i + "=" + bcmvinsns.get(i));
        //        }
                           
        removeIntLiterals(bcmv).makeSubstitution(bcmv);
        removeFloatLiterals(bcmv).makeSubstitution(bcmv);
    }

}
