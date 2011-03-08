/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class RemoveLiteralCoercions {

    private static boolean noisy = false;

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
                                   "(I)L" + intLiteral + ";", "targetedForRemoval"));
        matches.add(new LabelInsn("LabelInsn", new Label(), "targetedForRemoval"));
        matches.add(new VisitLineNumberInsn("visitlinenumber", 0, new Label(), "targetedForRemoval"));
        matches.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, "fortress/CompilerBuiltin", 
                                             "coerce_ZZ32", 
                                   "(Lfortress/CompilerBuiltin$IntLiteral;)L" + ZZ32 + ";", "targetedForRemoval"));
        ArrayList<Insn> replacements = new ArrayList<Insn>();
        replacements.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, 
                                            ZZ32,
                                            "make",
                                        "(I)L" + ZZ32 + ";", "ReplacementInsn"));
        return new Substitution(matches, replacements);
    }

    public static Substitution removeFloatLiterals(ByteCodeMethodVisitor bcmv) {
        String floatLiteral = "com/sun/fortress/compiler/runtimeValues/FFloatLiteral";
        String RR64 = "com/sun/fortress/compiler/runtimeValues/FRR64";
        ArrayList<Insn> matches = new ArrayList<Insn>();
        matches.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC,
                                             floatLiteral, 
                                             "make",
                                   "(D)L" + floatLiteral + ";", "targetedForRemoval"));
        matches.add(new LabelInsn("LabelInsn", new Label(), "targetedForRemoval"));
        matches.add(new VisitLineNumberInsn("visitlinenumber", 0, new Label(), "targetedForRemoval"));
        matches.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, "fortress/CompilerBuiltin", 
                                             "coerce_RR64", 
                                   "(Lfortress/CompilerBuiltin$FloatLiteral;)L" + RR64 + ";", "targetedForRemoval"));
        ArrayList<Insn> replacements = new ArrayList<Insn>();
        replacements.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, 
                                            RR64,
                                            "make",
                                        "(D)L" + RR64 + ";", "ReplacementInsn"));
        return new Substitution(matches, replacements);
    }

    // Sometimes we use a different idiom in code generation
    public static Substitution removeFloatLiterals2(ByteCodeMethodVisitor bcmv) {
        String floatLiteral = "com/sun/fortress/compiler/runtimeValues/FFloatLiteral";
        String RR64 = "com/sun/fortress/compiler/runtimeValues/FRR64";
        ArrayList<Insn> matches = new ArrayList<Insn>();
        matches.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC,
                                             floatLiteral, 
                                             "make",
                                   "(D)L" + floatLiteral + ";", "targetedForRemoval"));
        matches.add(new VarInsn("ASTORE", Opcodes.ASTORE, 0, "targetedForRemoval"));
        matches.add(new LabelInsn("LabelInsn", new Label(), "targetedForRemoval"));
        matches.add(new VisitLineNumberInsn("visitlinenumber", 0, new Label(), "targetedForRemoval"));
        matches.add(new VarInsn("ALOAD", Opcodes.ALOAD, 0, "targetedForRemoval"));
        matches.add(new LabelInsn("LabelInsn", new Label(), "targetedForRemoval"));
        matches.add(new VisitLineNumberInsn("visitlinenumber", 0, new Label(), "targetedForRemoval"));
        matches.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, "fortress/CompilerBuiltin", 
                                             "coerce_RR64", 
                                   "(Lfortress/CompilerBuiltin$FloatLiteral;)L" + RR64 + ";", "targetedForRemoval"));
        ArrayList<Insn> replacements = new ArrayList<Insn>();
        replacements.add(new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, 
                                            RR64,
                                            "make",
                                        "(D)L" + RR64 + ";", "ReplacementInsn"));
        return new Substitution(matches, replacements);
    }


    public static void removeCoercions(ByteCodeMethodVisitor bcmv) {
        if (noisy) {
            System.out.println(" removing Coercions");
            bcmv.printInsns(bcmv.insns, "removeCoercions");
        }
        removeIntLiterals(bcmv).makeSubstitution(bcmv);
        removeFloatLiterals(bcmv).makeSubstitution(bcmv);
        removeFloatLiterals2(bcmv).makeSubstitution(bcmv);
    }
}
