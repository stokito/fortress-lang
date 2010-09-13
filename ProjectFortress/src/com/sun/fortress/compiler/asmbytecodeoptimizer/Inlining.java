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

    public static boolean isBuiltinStaticMethod(ByteCodeMethodVisitor bcmv, Insn insn) {
        if (insn instanceof MethodInsn) {
            MethodInsn mi = (MethodInsn) insn;
            if ((mi.opcode == Opcodes.INVOKESTATIC) && (isCompilerBuiltin(mi.owner))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBuiltinInterfaceMethod(ByteCodeMethodVisitor bcmv, Insn insn) {
        if (insn instanceof MethodInsn) {
            MethodInsn mi = (MethodInsn) insn;
            if ((mi.opcode == Opcodes.INVOKEINTERFACE) && (isCompilerBuiltin(mi.owner))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBuiltinInstanceMethod(ByteCodeMethodVisitor bcmv, Insn insn) {
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

    public static List<Insn> convertInsn(Insn i, int[] args) {
         if (noisy) {
             System.out.println("convert INSN insn = = " + i);
             for (int j = 0; j < args.length; j++)
                 System.out.println(" args " + j + " = " + args[j]);
         }
        List<Insn> result = new ArrayList<Insn>();
        if (i instanceof SingleInsn) {
            SingleInsn si = (SingleInsn) i;
            switch(si.opcode) {
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ARETURN:
            case Opcodes.RETURN:  break;
            default: result.add(i);
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
            case Opcodes.ASTORE: result.add(new VarInsn(vi.name, vi.opcode, args[vi.var])); break;
            default: result.add(i);
            }
        } else if (i instanceof VisitMaxs) {
        } else if (i instanceof VisitEnd) {
        } else {
            result.add(i);
        }

        if (noisy) System.out.println("convert INSN result = " + result);
        return result;
    }


    private static int argNameCount = 0;

    private static String gensym(int i) {
        return "Argument" + i + "_" + argNameCount++;
    }

    private static void printMethod(ByteCodeMethodVisitor method, String when) {

        if (noisy) {
            System.out.println("Printing Method: " + method.name + "  " + when + " with maxStack = " + method.maxStack + " and maxLocals = " + method.maxLocals);
            printInsns(method.insns, " ");
            System.out.println("End Method: " + method.name);
        }
    }

    private static void printInsns(List<Insn> insns, String header) {

            for (Insn inst : insns) {
                if (inst.isExpanded()) {
                    System.out.println(header + "Replaced Bytecode: " + inst);
                    printInsns(inst.inlineExpansionInsns, header + " ");
                } else {
                    System.out.println(header + "Bytecode: " + inst + inst.getStackString() + inst.getLocals());
                }
            }
    }


    // For Debugging
    private static void printRecursivelyInlinedInsns(List<Insn> insns, int depth) {
        for (Insn inst : insns) {
            if (inst.isExpanded()) {
                printInsns(inst.inlineExpansionInsns, depth++);
            }
            if (depth == 400) {
                System.out.println("XXXXXXXXXXXXXXXXXX");
                String header = "";
                while (inst.parentInsn != null) {
                    System.out.println( header + inst );
                    inst = inst.parentInsn;
                    header = header + " ";
                }
                System.exit(1);
            }
        }
    }

    private static void printInsns(List<Insn> insns, int depth) {
        for (Insn inst : insns) {
            if (inst.isExpanded()) {
                System.out.println("(" + depth + ") Replaced Bytecode: " + inst);
                printInsns(inst.inlineExpansionInsns, depth++);
            }
            System.out.println("(" + depth + ") Bytecode: " + inst + inst.getStackString() + inst.getLocals());
        }
    }

    public static void recalculateLabels(ByteCodeMethodVisitor method) {
        method.labelNames = new HashMap();
        recalculateLabels(method, method.insns, 0);
    }

    public static void recalculateLabels(ByteCodeMethodVisitor method, List<Insn> insns, int current) {

        for (Insn insn : insns) {
            if (insn instanceof LabelInsn) {
                LabelInsn labelInsn = (LabelInsn) insn;
                method.labelNames.put(labelInsn.label.toString(), new Integer(current++));
            } else if (insn.isExpanded()) {
                recalculateLabels(method, insn.inlineExpansionInsns, current);
            } else {
                current++;
            }
        }
    }

    public static void inlineStaticMethodCall(String className, ByteCodeMethodVisitor method, MethodInsn mi, ByteCodeMethodVisitor methodToInline) {
        List<Insn> insns = new ArrayList<Insn>();
        Label start = new Label();
        Label end = new Label();

        insns.add(new LabelInsn("start", start));
        int[] offsets = new int[methodToInline.maxLocals];
        int locals = method.maxLocals;
        method.maxStack = method.maxStack + methodToInline.maxStack;

        for (int i = methodToInline.args.size() - 1; i >= 0; i--) {
            insns.add(new VarInsn("ASTORE", Opcodes.ASTORE, method.maxLocals + i));
            offsets[i] = method.maxLocals + i;
        }

        method.maxLocals = method.maxLocals + methodToInline.args.size();
        for (int i = 0; i < methodToInline.insns.size(); i++) {
            insns.addAll(convertInsn(methodToInline.insns.get(i), offsets));
        }

        insns.add(new LabelInsn("end", end));

        for (Insn insn : insns) {
            insn.parentInsn = mi;
        }

        mi.inlineExpansionInsns.addAll(insns);
        

        //        method.insns.remove(index);
        //        method.insns.addAll(index, insns);
        //        recalculateLabels(method);

        if (noisy) {
            System.out.println("Method:"+ method.name + " Replacing " + mi + " with");
            for (Insn i : insns) {
                System.out.println("\t" + i);
            }

            printMethod(method, "During");
        }

    }
            
    public static void inlineNonStaticMethodCall(String className, ByteCodeMethodVisitor method, MethodInsn mi, ByteCodeMethodVisitor methodToInline) {
        List<Insn> insns = new ArrayList<Insn>();
        Label start = new Label();
        Label end = new Label();

        insns.add(new LabelInsn("start", start));
        int[] offsets = new int[methodToInline.maxLocals];
        int locals = method.maxLocals;
        method.maxStack = method.maxStack + methodToInline.maxStack;

        for (int i = methodToInline.args.size(); i >= 0; i--) {
            insns.add(new VarInsn("ASTORE", Opcodes.ASTORE, method.maxLocals + i));
            offsets[i] = method.maxLocals + i;
        }

        method.maxLocals = method.maxLocals + methodToInline.args.size();

        for (int i = 0; i < methodToInline.insns.size(); i++) {
            insns.addAll(convertInsn(methodToInline.insns.get(i), offsets));
        }

        insns.add(new LabelInsn("end", end));

        for (Insn insn : insns) {
            insn.parentInsn = mi;
        }

        mi.inlineExpansionInsns.addAll(insns);

//         method.insns.remove(index);
//         method.insns.addAll(index, insns);
//         recalculateLabels(method);

        if (noisy) {
            System.out.println("Method:"+ method.name + " Replacing " + mi + " with");
            for (Insn i : insns) {
                System.out.println("\t" + i);
            }
        }
    }

    public static void Inline(ByteCodeMethodVisitor bcmv, String className) {
        ByteCodeOptimizer builtin = new ByteCodeOptimizer();
        builtin.readInJarFile("default_repository/caches/bytecode_cache/fortress.CompilerBuiltin.jar");
        IterateOverInsns(bcmv, className, builtin, bcmv.insns);
        recalculateLabels(bcmv);
    }

    public static boolean isNotInParentSet(MethodInsn mi) {
            MethodInsn current = mi;

            while (current.parentInsn != null) {
                if ((mi._name.equals(current._name) && (mi.desc.equals(current.desc)))) {
                    return false;
                }
                current = (MethodInsn) current.parentInsn;
            }
            return true;
    }
                                                       
                                                   


    public static void IterateOverInsns(ByteCodeMethodVisitor bcmv, String className, ByteCodeOptimizer builtin, List<Insn> insns) {
        for (Insn insn : insns) {

            // Come back and fix this later.  Check for CompilerBuiltin first, and then check if static, interface, or instance.
            if (insn instanceof MethodInsn) {
                MethodInsn mi = (MethodInsn) insn;
                if (isBuiltinInterfaceMethod(bcmv, insn)) {
                    ByteCodeVisitor bcv = (ByteCodeVisitor) builtin.classes.get(mi.owner + "$DefaultTraitMethods.class");
                    // Revisit this to see how kosher it is. Encoding knowledge of how default methods work.
                    ByteCodeMethodVisitor methodToInline = (ByteCodeMethodVisitor) bcv.methodVisitors.get(mi._name + 
                                                                                                          mi.desc); 
                    if (methodToInline != null && isNotInParentSet(mi)) {
                        inlineNonStaticMethodCall(className, bcmv, mi, methodToInline);
                        //                    IterateOverInsns(bcmv, className, builtin, mi.inlineExpansionInsns);
                    }
                } else if (isCompilerBuiltin(mi.owner)) {
                    if (isBuiltinStaticMethod(bcmv, insn)) {
                        ByteCodeVisitor bcv = (ByteCodeVisitor) builtin.classes.get(mi.owner + ".class");
                        ByteCodeMethodVisitor methodToInline = (ByteCodeMethodVisitor) bcv.methodVisitors.get(mi._name + 
                                                                                                              mi.desc); 
                        if (methodToInline != null && isNotInParentSet(mi)) {
                            inlineStaticMethodCall(className, bcmv, mi, methodToInline);
                            //    IterateOverInsns(bcmv, className, builtin, mi.inlineExpansionInsns);
                        }
                    } else if (isBuiltinInstanceMethod(bcmv, insn)) {
                    }
                } else if (isNativeInterface(mi.owner)) {
                }
            }
        }
    }
}