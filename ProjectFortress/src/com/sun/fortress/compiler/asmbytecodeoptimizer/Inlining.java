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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class Inlining {

    private static boolean noisy = false;

    public static void optimize(ByteCodeVisitor bcv) {
        Iterator it = bcv.methodVisitors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            ByteCodeMethodVisitor bcmv = (ByteCodeMethodVisitor) pairs.getValue();
            String className = (String) pairs.getKey();
            Inline(bcmv, className);
        }
    }


    public static boolean isCompilerBuiltin(String name) {
        return name.startsWith("fortress/CompilerBuiltin");
    }


    public static boolean isBuiltinStaticMethod(ByteCodeMethodVisitor bcmv, int i) {
        Insn insn = bcmv.insns.get(i);
        if (insn instanceof MethodInsn) {
            MethodInsn mi = (MethodInsn) insn;
            if ((mi.opcode == Opcodes.INVOKESTATIC) && (isCompilerBuiltin(mi.owner))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBuiltinInterfaceMethod(ByteCodeMethodVisitor bcmv, int i) {
        Insn insn = bcmv.insns.get(i);
        if (insn instanceof MethodInsn) {
            MethodInsn mi = (MethodInsn) insn;
            if ((mi.opcode == Opcodes.INVOKEINTERFACE) && (isCompilerBuiltin(mi.owner))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBuiltinInstanceMethod(ByteCodeMethodVisitor bcmv, int i) {
        Insn insn = bcmv.insns.get(i);
        if (insn instanceof MethodInsn) {
            MethodInsn mi = (MethodInsn) insn;
            if ((mi.opcode == Opcodes.INVOKEVIRTUAL) && (isCompilerBuiltin(mi.owner))) {
                return true;
            }
        }
        return false;
    }


    public static boolean isNativeInterface(String methodName) {
        return false;
    }

    // We need to convert 
    public static Insn convertInsn(Insn i, int[] args) {
        if (i instanceof SingleInsn) {
            SingleInsn si = (SingleInsn) i;
            switch(si.opcode) {
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ARETURN:
            case Opcodes.RETURN: return new SingleInsn("NOP", Opcodes.NOP); 
            default: return i;
            }
        } else if (i instanceof VarInsn) {
            VarInsn vi = (VarInsn) i;
            switch(vi.opcode) {
            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD: 
            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE: return new VarInsn(vi.name, vi.opcode, args[vi.var]);
            default: return i;
            }
        } else if (i instanceof VisitMaxs) {
            return new SingleInsn("NOP", Opcodes.NOP);
        }
        return i;
    }


    private static int argNameCount = 0;

    private static String gensym(int i) {
        return "Argument" + i + "_" + argNameCount++;
    }

    private static void printMethod(ByteCodeMethodVisitor method, String when) {

        if (noisy) {
            System.out.println("Printing Method: " + method.name + "  " + when);

            for (Insn inst : method.insns) {
                System.out.println("Bytecode: " + inst + inst.getStackString() + inst.getLocals());
            }
        
            System.out.println("End Method: " + method.name);
        }
    }

    public static void recalculateLabels(ByteCodeMethodVisitor method) {
        method.labelNames = new HashMap();
        for (int i = 0; i < method.insns.size() -1; i++)
            {
                Insn insn = method.insns.get(i);
                if (insn instanceof LabelInsn) {
                    LabelInsn labelInsn = (LabelInsn) insn;
                    method.labelNames.put(labelInsn.label.toString(), new Integer(i));
                }
            }
    }

    public static void inlineInsn(String className, ByteCodeMethodVisitor method, MethodInsn mi, ByteCodeMethodVisitor methodToInline, int index) {
        printMethod(method, "BEFORE");

        List<Insn> insns = new ArrayList<Insn>();
        Label start = new Label();
        Label end = new Label();

        insns.add(new LabelInsn("start", start));
        String[] args = new String[methodToInline.args.size()];
        int[] offsets = new int[methodToInline.args.size()];

        int locals = method.maxLocals;

        for (int i = methodToInline.args.size()-1; i >=0; i--) {
            insns.add(new VarInsn("ASTORE", Opcodes.ASTORE, method.maxLocals));
            offsets[i] = method.maxLocals++;
        }
                      
        for (int i = 0; i < methodToInline.insns.size() - 1; i++) {
            insns.add(convertInsn(methodToInline.insns.get(i), offsets));
        }

        insns.add(new LabelInsn("end", end));
        for (int i = 0; i < methodToInline.args.size(); i++) {
            insns.add(new LocalVariableInsn("FRED", gensym(locals), methodToInline.args.get(i), "", start, end, locals));
        }

        method.insns.remove(index);
        method.insns.addAll(index, insns);
        recalculateLabels(method);

        AbstractInterpretation.optimizeMethod(className, method);
        printMethod(method, "AFTER:");

    }



    public static void Inline(ByteCodeMethodVisitor bcmv, String className) {
        ByteCodeOptimizer builtin = new ByteCodeOptimizer();
        builtin.readInJarFile("default_repository/caches/bytecode_cache/fortress.CompilerBuiltin.jar");

        int maxLocals = bcmv.maxLocals;

        boolean changed = true;
        //        while (changed == true) {
        changed = false;
        for (int i = 0; i < bcmv.insns.size(); i++) {
            Insn insn = bcmv.insns.get(i);
            if (insn instanceof MethodInsn) {
                MethodInsn mi = (MethodInsn) insn;
                if (isBuiltinInterfaceMethod(bcmv, i)) {

                } else if (isCompilerBuiltin(mi.owner)) {
                    if (isBuiltinStaticMethod(bcmv, i)) {
                        ByteCodeVisitor bcv = (ByteCodeVisitor) builtin.classes.get(mi.owner + ".class");
                        ByteCodeMethodVisitor methodToInline = (ByteCodeMethodVisitor) bcv.methodVisitors.get(mi._name + 
                                                                                                              mi.desc); 
                        if (methodToInline != null) {
                            inlineInsn(className, bcmv, mi, methodToInline, i);
                            changed = true;
                        }
                    } else if (isBuiltinInstanceMethod(bcmv, i)) {
                    }
                } else if (isNativeInterface(mi.owner)) {


                }
            }
        }
        //        }
    }
}