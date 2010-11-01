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

    private static boolean noisy = true;

    public static void fixJumpInsns() {
        Iterator it = InlinedJumpInsns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            JumpInsn ji = (JumpInsn) pairs.getKey();
            Label l = (Label) pairs.getValue();
            LabelInsn newLabel = getLabel(l);
            ji.label = newLabel.label;
        }
    }

    public static void optimize(ByteCodeVisitor bcv) {
        Iterator it = bcv.methodVisitors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            ByteCodeMethodVisitor bcmv = (ByteCodeMethodVisitor) pairs.getValue();
            String className = (String) pairs.getKey();
            InlinedMethodLabels = new HashMap();
            InlinedJumpInsns = new HashMap();
            Inline(bcmv, className);
            fixJumpInsns();
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


    public static boolean isBoxedNativeInterfaceMethod(String methodName) {
        if (methodName.contains("nativeHelpers") && methodName.startsWith("native"))
            return true;
        return false;
    }

    public static boolean isUnboxedNativeInterfaceMethod(String methodName) {
        if (methodName.contains("nativeHelpers") && methodName.startsWith("com"))
            return true;
        return false;

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

    static HashMap InlinedMethodLabels;
    static HashMap InlinedJumpInsns;

    static void addLabel(Label label, LabelInsn labelInsn) { 
        InlinedMethodLabels.put(label, labelInsn); 
    }

    static LabelInsn getLabel(Label label) { 
        LabelInsn result = (LabelInsn) InlinedMethodLabels.get(label); 
        return result; 
    }

    static void addJump(JumpInsn JumpInsn, Label label) {
        InlinedJumpInsns.put(JumpInsn, label);
    }

    static String newIndex(MethodInsn mi, int index) { return mi.index + "." + index;}

    public static List<Insn> convertInsns(MethodInsn mi, List<Insn> insns, int[] args, int _index) {
        List<Insn> result = new ArrayList<Insn>();
        int index = _index;
        for (Insn i : insns) {
            if (i instanceof LabelInsn) {
                LabelInsn oldLabelInsn = (LabelInsn) i;
                String newName = oldLabelInsn.name + mi.index;
                LabelInsn newLabelInsn = new LabelInsn(newName, new Label(), newIndex(mi,index++));
                addLabel(oldLabelInsn.label, newLabelInsn);
                result.add(newLabelInsn);
            } else if (i instanceof JumpInsn)  {
                JumpInsn oldJumpInsn = (JumpInsn) i; 
                JumpInsn newJumpInsn = 
                    new JumpInsn(oldJumpInsn.name, oldJumpInsn.opcode, 
                                 oldJumpInsn.label, newIndex(mi,index++));
                addJump(newJumpInsn, oldJumpInsn.label);
                result.add(newJumpInsn);
            } else if (i instanceof VisitLineNumberInsn) {
                VisitLineNumberInsn oldVisitLineNumberInsn = (VisitLineNumberInsn) i;
                LabelInsn oldLabelInsn = getLabel(oldVisitLineNumberInsn.start);
                VisitLineNumberInsn newVisitLineNumberInsn =                     
                    new VisitLineNumberInsn(oldVisitLineNumberInsn.name,
                                            oldVisitLineNumberInsn.line,
                                            oldLabelInsn.label,
                                            newIndex(mi,index++));
                result.add(newVisitLineNumberInsn);
            } else if (i instanceof LocalVariableInsn) {
                LocalVariableInsn oldLocalVariableInsn = (LocalVariableInsn) i;
                LabelInsn oldStartLabel = getLabel(oldLocalVariableInsn.start);
                LabelInsn oldEndLabel = getLabel(oldLocalVariableInsn.end);
                LocalVariableInsn newLocalVariableInsn = 
                    new LocalVariableInsn(oldLocalVariableInsn.name,
                                          oldLocalVariableInsn._name,
                                          oldLocalVariableInsn.desc,
                                          oldLocalVariableInsn.sig,
                                          oldStartLabel.label,
                                          oldEndLabel.label,
                                          args[oldLocalVariableInsn._index],
                                          newIndex(mi, index++));
                result.add(newLocalVariableInsn);
                
            } else  if (i instanceof SingleInsn) {
                SingleInsn si = (SingleInsn) i;
                switch(si.opcode) {
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                case Opcodes.FRETURN:
                case Opcodes.DRETURN:
                case Opcodes.ARETURN:
                case Opcodes.RETURN:  result.add(new SingleInsn("NOP", Opcodes.NOP, newIndex(mi, index++))); break;
                default: result.add(i.copy(newIndex(mi, index++)));
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
                case Opcodes.ASTORE: 
                    VarInsn newVarInsn = new VarInsn(vi.name, vi.opcode, args[vi.var], newIndex(mi, index++));
                    result.add(newVarInsn);
                    break;
                default: result.add(i.copy(newIndex(mi, index++)));
                }
            } else if (i instanceof VisitMaxs) {
                result.add(new SingleInsn("NOP", Opcodes.NOP, newIndex(mi, index++)));
            } else if (i instanceof VisitEnd) {
                result.add(new SingleInsn("NOP", Opcodes.NOP, newIndex(mi, index++)));
            } else if (i instanceof VisitCode) {
                result.add(new SingleInsn("NOP", Opcodes.NOP, newIndex(mi, index++)));
            } else if (i instanceof VisitFrame) {
                result.add(new SingleInsn("NOP", Opcodes.NOP, newIndex(mi, index++)));
            } else {
                    result.add(i.copy(newIndex(mi, index++)));
            }
        }

        return result;
    }

    public static void inlineMethodCall(String className, ByteCodeMethodVisitor method, 
                                        MethodInsn mi, ByteCodeMethodVisitor methodToInline, 
                                        Boolean staticMethod) {
        List<Insn> insns = new ArrayList<Insn>();
        Label start = new Label();
        Label end = new Label();
        int argsStart = methodToInline.args.size();
        int localsStart = methodToInline.maxLocals;
        int index = 0;
        insns.add(new LabelInsn("start_"+ mi._name, start, newIndex(mi, index++)));
        int[] offsets = new int[methodToInline.maxLocals + 1];
        int locals = method.maxLocals;

    // Static Method start at args.size() - 1, NonStatic Methods start at args.size()
        if (staticMethod) argsStart = argsStart - 1;

        for (int i = argsStart; i >= 0; i--) {
            insns.add(new VarInsn("ASTORE", Opcodes.ASTORE, method.maxLocals + i, newIndex(mi, index++)));
            offsets[i] = method.maxLocals + i;
        }

        for (int i = localsStart-1; i > argsStart; i--)
            offsets[i] = method.maxLocals+i;

        method.maxStack = java.lang.Math.max(method.maxStack, methodToInline.maxStack);
        method.maxLocals = method.maxLocals + methodToInline.args.size();

        insns.addAll(convertInsns(mi, methodToInline.insns, offsets, index));

        insns.add(new LabelInsn("end_" + mi._name, end, newIndex(mi, insns.size())));

        for (Insn insn : insns) {
            insn.parentInsn = mi;
        }


        mi.inlineExpansionInsns.addAll(insns);
    }
                
    public static void Inline(ByteCodeMethodVisitor bcmv, String className) {
        ByteCodeOptimizer builtin = new ByteCodeOptimizer();
        builtin.readInJarFile("default_repository/caches/bytecode_cache/fortress.CompilerBuiltin.jar");
        ByteCodeOptimizer nativeHelpers = new ByteCodeOptimizer();
        nativeHelpers.readInNativeClassFiles();
        IterateOverInsns(bcmv, className, builtin, nativeHelpers, bcmv.insns, "");
        recalculateLabels(bcmv);
    }

    public static boolean isNotInParentSet(MethodInsn mi) {
        MethodInsn current = (MethodInsn) mi.parentInsn;

            while (current != null) {
                if ((mi._name.equals(current._name) && (mi.desc.equals(current.desc)))) {
                    return false;
                }
                current = (MethodInsn) current.parentInsn;
            }
            return true;
    }

    static int iterationCount = 0;
    public static void IterateOverInsns(ByteCodeMethodVisitor bcmv, String className, ByteCodeOptimizer builtin, 
                                        ByteCodeOptimizer nativeHelpers,  ArrayList<Insn> insns, String preamble) {
        for (Insn insn : insns) {
            // We have an issue with inlining methods which have already inlined methods with labels.
            // By walking over all of the bytecodes we end up fixing up all of the labels
            // For now, throw out other expansions.
            if (insn instanceof MethodInsn) {
                    MethodInsn mi = (MethodInsn) insn;
                    if (isBuiltinInterfaceMethod(bcmv, insn)) {
                        ByteCodeVisitor bcv = (ByteCodeVisitor) builtin.classes.get(mi.owner + "$DefaultTraitMethods.class");
                        // Revisit this to see how kosher it is. Encoding knowledge of how default methods work.
                        ByteCodeMethodVisitor methodToInline = (ByteCodeMethodVisitor) bcv.methodVisitors.get(mi._name + 
                                                                                                              mi.desc); 
                        if (methodToInline != null && isNotInParentSet(mi)) {
                            inlineMethodCall(className, bcmv, mi, methodToInline, false);
                            IterateOverInsns(bcmv, className, builtin, nativeHelpers, mi.inlineExpansionInsns, preamble + "    ");
                        }
                    } else if (isCompilerBuiltin(mi.owner)) {
                        if (isBuiltinStaticMethod(bcmv, insn)) {
                            ByteCodeVisitor bcv = (ByteCodeVisitor) builtin.classes.get(mi.owner + ".class");
                            ByteCodeMethodVisitor methodToInline = (ByteCodeMethodVisitor) bcv.methodVisitors.get(mi._name + 
                                                                                                                  mi.desc); 
                            if (methodToInline != null && isNotInParentSet(mi)) {
                                inlineMethodCall(className, bcmv, mi, methodToInline,true);
                                IterateOverInsns(bcmv, className, builtin, nativeHelpers, mi.inlineExpansionInsns, preamble + "    ");
                            }
                        } else if (isBuiltinInstanceMethod(bcmv, insn)) {
                        }
                    } else if (isBoxedNativeInterfaceMethod(mi.owner)) {
                        String key = mi.owner + ".class";
                        ByteCodeVisitor bcv = (ByteCodeVisitor) nativeHelpers.classes.get(key);
                        ByteCodeMethodVisitor methodToInline = (ByteCodeMethodVisitor) bcv.methodVisitors.get(mi._name + 
                                                                                                              mi.desc); 
                        if (methodToInline != null) {
                            inlineMethodCall(className, bcmv, mi, methodToInline, true);
                            IterateOverInsns(bcmv, className, builtin, nativeHelpers, mi.inlineExpansionInsns, preamble + "    ");
                        }

                    } else if (isUnboxedNativeInterfaceMethod(mi.owner)) {
                        String key = "native/" + mi.owner + ".class";
                        ByteCodeVisitor bcv = (ByteCodeVisitor) nativeHelpers.classes.get(key);
                        ByteCodeMethodVisitor methodToInline = (ByteCodeMethodVisitor) bcv.methodVisitors.get(mi._name + 
                                                                                                              mi.desc); 
                        if (methodToInline != null) {
                            inlineMethodCall(className, bcmv, mi, methodToInline, true);
                            IterateOverInsns(bcmv, className, builtin, nativeHelpers, mi.inlineExpansionInsns, preamble + "    ");
                        }
                    }
            }
        }
    }
}
