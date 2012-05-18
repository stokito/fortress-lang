/*******************************************************************************
    Copyright 2010,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
            bcmv.labelDefs = new HashMap();
            Inline(bcmv, className);
        }
    }

    public static boolean isCompilerBuiltin(String name) {
        return name.startsWith("fortress/CompilerBuiltin");
    }

    public static boolean isCompilerLibrary(String name) {
        return name.startsWith("fortress/CompilerLibrary");
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

    public static boolean isLibraryStaticMethod(ByteCodeMethodVisitor bcmv, Insn insn) {
        if (insn instanceof MethodInsn) {
            MethodInsn mi = (MethodInsn) insn;
            if ((mi.opcode == Opcodes.INVOKESTATIC) && (isCompilerLibrary(mi.owner))) {
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

    public static boolean isLibraryInstanceMethod(ByteCodeMethodVisitor bcmv, Insn insn) {
        if (insn instanceof MethodInsn) {
            MethodInsn mi = (MethodInsn) insn;
            if ((mi.opcode == Opcodes.INVOKEVIRTUAL) && (isCompilerLibrary(mi.owner))) {
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

    public static boolean isGenericMethod(String methodOwner) {
        if (methodOwner.contains("\u27e6"))
            return true;
        return false;
    }

    private static void printMethod(ByteCodeMethodVisitor method, String when) {
        if (noisy) {
            System.out.println("Printing Method: " + method.name + "  " + when + " with maxStack = " + method.maxStack + " and maxLocals = " + method.maxLocals);
            printInsns(method.insns, " ", 0);
            System.out.println("End Method: " + method.name);
        }
    }

    private static void printInsns(List<Insn> insns, String header, int counter) {
        int i = counter;
        for (Insn inst : insns) {
            if (inst.isExpanded()) {
                System.out.println(header + "Replaced Bytecode: " + inst + " ::: " + inst.index);
                printInsns(inst.inlineExpansionInsns, header + " ", i);
            } else {
                System.out.println(header + "[" + i++ + "]" + "Bytecode: " + inst + ":::" + inst.index + ":::" + inst.getStackString() + inst.getLocals());
            }
        }
    }

     public static void recalculateLabels(ByteCodeMethodVisitor method) {
         recalculateLabels(method, method.insns);
     }

     public static void recalculateLabels(ByteCodeMethodVisitor method, List<Insn> insns) {
         int count = 0;
         for (Insn insn : insns) {
             if (insn instanceof LabelInsn) {
                 LabelInsn labelInsn = (LabelInsn) insn;
                 method.labelDefs.put(labelInsn.label.toString(), new Integer(count));
             } else if (insn.isExpanded()) {
                 throw new RuntimeException("expanded insns shouldn't get here");
                 //                     recalculateLabels(method, insn.inlineExpansionInsns);
               }
             count++;
         }
     }


    static String newIndex(MethodInsn mi, int index) { return mi.index + "." + index;}

    public static List<Insn> convertInsns(MethodInsn mi, List<Insn> insns, int[] args, int _index, Label end) {
        List<Insn> result = new ArrayList<Insn>();
        HashMap labels = new HashMap();
        int index = _index;
        for (Insn i : insns) {
            if (i.isExpanded()) {
                MethodInsn expanded = (MethodInsn) i;
                // This use of end should be OK because all returns should have been removed when inlined before.
                // What could go wrong?
                result.addAll(convertInsns(expanded, expanded.inlineExpansionInsns, args, _index, end));
            } else if (i instanceof SingleInsn) {
                SingleInsn si = (SingleInsn) i;
                switch(si.opcode) {
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                case Opcodes.FRETURN:
                case Opcodes.DRETURN:
                case Opcodes.ARETURN:
                case Opcodes.RETURN:  result.add(new JumpInsn("RETURN->GOTO", Opcodes.GOTO, end, newIndex(mi, index++))); break;
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
            } else if (i instanceof VisitEnd) {
            } else if (i instanceof VisitCode) {
            } else if (i instanceof VisitFrame) {
            } else if (i instanceof LabelInsn) {
                LabelInsn li = (LabelInsn) i;
                if (labels.containsKey(li.label))
                    result.add(new LabelInsn(li.name,(Label) labels.get(li.label), newIndex(mi, index++)));
                else {
                    Label l = new Label();
                    labels.put(li.label, l);
                    result.add(new LabelInsn(li.name, l, newIndex(mi, index++)));
                }
            } else if (i instanceof JumpInsn) {
                JumpInsn ji = (JumpInsn) i;
                if (labels.containsKey(ji.label)) 
                    result.add(new JumpInsn(ji.name, ji.opcode, (Label) labels.get(ji.label), newIndex(mi, index++)));
                else {
                    Label l = new Label();
                    labels.put(ji.label, l);
                    result.add(new JumpInsn(ji.name, ji.opcode, l, newIndex(mi, index++)));
                }
            } else if (i instanceof VisitLineNumberInsn) {
                VisitLineNumberInsn vlni = (VisitLineNumberInsn) i;
                if (labels.containsKey(vlni.start))
                    result.add(new VisitLineNumberInsn(vlni.name, vlni.line,
                                                       (Label) labels.get(vlni.start), newIndex(mi,index++)));
                else {
                    Label l = new Label();
                    labels.put(vlni.start, l);
                    result.add(new VisitLineNumberInsn(vlni.name, vlni.line, l, newIndex(mi, index++)));
                }
            } else if (i instanceof LocalVariableInsn) {
                LocalVariableInsn lvi = (LocalVariableInsn) i;
                if (labels.containsKey(lvi.start) && labels.containsKey(lvi.end)) {
                    result.add(new LocalVariableInsn(lvi.name, lvi._name, lvi.desc, lvi.sig, (Label) labels.get(lvi.start), 
                                                     (Label) labels.get(lvi.end), args[lvi._index], newIndex(mi, index++)));
                } else throw new RuntimeException("NYI");
            } else if (i instanceof TryCatchBlock) {
                TryCatchBlock tcb = (TryCatchBlock) i;
                if (labels.containsKey(tcb.start) && labels.containsKey(tcb.end) && labels.containsKey(tcb.handler)) {
                    result.add(new TryCatchBlock(tcb.name, (Label) labels.get(tcb.start), (Label) labels.get(tcb.end),
                                                 (Label) labels.get(tcb.handler), tcb.type, newIndex(mi, index++)));
                } else if (!labels.containsKey(tcb.start) && !labels.containsKey(tcb.end) && 
                           !labels.containsKey(tcb.handler)) {
                    Label s = new Label();
                    Label e = new Label();
                    Label h = new Label();
                    labels.put(tcb.start, s);
                    labels.put(tcb.end, e);
                    labels.put(tcb.handler, h);
                    result.add(new TryCatchBlock(tcb.name, s, e, h, tcb.type, newIndex(mi, index++)));
                } else throw new RuntimeException("NYI");
                // Need to add TableSwitch, LookupSwitch
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

        // Static Method start at args.size() - 1, NonStatic Methods start at args.size()
        if (staticMethod) argsStart = argsStart - 1;

        for (int i = argsStart; i >= 0; i--) {
            insns.add(new VarInsn("ASTORE", Opcodes.ASTORE, method.maxLocals + i, newIndex(mi, index++)));
            offsets[i] = method.maxLocals + i;
        }

        for (int i = localsStart-1; i > argsStart; i--)
            offsets[i] = method.maxLocals+i;

        method.maxStack = method.maxStack + methodToInline.maxStack;
        method.maxLocals = method.maxLocals + methodToInline.args.size() + methodToInline.maxLocals;
        insns.addAll(convertInsns(mi, methodToInline.insns, offsets, index, end));
        insns.add(new LabelInsn("end_" + mi._name, end, newIndex(mi, insns.size())));

        for (Insn insn : insns) {
            insn.setParentInsn(mi);
        }


        mi.inlineExpansionInsns.addAll(insns);
    }

    public static ArrayList<Insn> flatten(ArrayList<Insn> insns) {
        ArrayList<Insn> fi = new ArrayList<Insn>();
        for (Insn i : insns) {
            if (i.isExpanded()) {
                MethodInsn mi = (MethodInsn) i;
                fi.addAll(flatten(mi.inlineExpansionInsns));
            } else {
                fi.add(i);
            }
        }
        return fi;
    }
        
                
    public static void Inline(ByteCodeMethodVisitor bcmv, String className) {
        ByteCodeOptimizer builtin = new ByteCodeOptimizer();
        builtin.readInJarFile("default_repository/caches/bytecode_cache/fortress.CompilerBuiltin.jar");
        ByteCodeOptimizer library = new ByteCodeOptimizer();
        library.readInJarFile("default_repository/caches/bytecode_cache/fortress.CompilerLibrary.jar");
        ByteCodeOptimizer nativeHelpers = new ByteCodeOptimizer();
        nativeHelpers.readInNativeClassFiles();
        IterateOverInsns(bcmv, className, builtin, library, nativeHelpers, bcmv.insns, "");
        bcmv.insns = flatten(bcmv.insns);
        recalculateLabels(bcmv);
    }

    public static boolean isNotInParentSet(MethodInsn mi) {
        MethodInsn current = (MethodInsn) mi.getParentInsn();

            while (current != null) {
                if ((mi._name.equals(current._name) && (mi.desc.equals(current.desc)))) {
                    return false;
                }
                current = (MethodInsn) current.getParentInsn();
            }
            return true;
    }

    public static void IterateOverInsns(ByteCodeMethodVisitor bcmv, String className, ByteCodeOptimizer builtin, 
                                        ByteCodeOptimizer library, ByteCodeOptimizer nativeHelpers,  
                                        ArrayList<Insn> insns, String preamble) {
        for (Insn insn : insns) {
            // We have an issue with inlining methods which have already inlined methods with labels.
            // By walking over all of the bytecodes we end up fixing up all of the labels
            // For now, throw out other expansions.
            if (insn instanceof MethodInsn) {
                MethodInsn mi = (MethodInsn) insn;
                if (isGenericMethod(mi.owner)) {
                    //  We can't do anything here
                } else if (isBuiltinInterfaceMethod(bcmv, insn)) {
                    ByteCodeVisitor bcv = (ByteCodeVisitor) builtin.classes.get(mi.owner + "$DefaultTraitMethods.class");
                    // Revisit this to see how kosher it is. Encoding knowledge of how default methods work.
                    ByteCodeMethodVisitor methodToInline = (ByteCodeMethodVisitor) bcv.methodVisitors.get(mi._name + 
                                                                                                          mi.desc); 
                    if (methodToInline != null && isNotInParentSet(mi)) {
                        inlineMethodCall(className, bcmv, mi, methodToInline, false);
                        IterateOverInsns(bcmv, className, builtin, library, nativeHelpers, mi.inlineExpansionInsns, preamble + "    ");
                    }
                } else if (isCompilerBuiltin(mi.owner)) {
                    if (isBuiltinStaticMethod(bcmv, insn)) {
                        ByteCodeVisitor bcv = (ByteCodeVisitor) builtin.classes.get(mi.owner + ".class");
                        ByteCodeMethodVisitor methodToInline = (ByteCodeMethodVisitor) bcv.methodVisitors.get(mi._name + 
                                                                                                              mi.desc); 
                        if (methodToInline != null && isNotInParentSet(mi)) {
                            inlineMethodCall(className, bcmv, mi, methodToInline,true);
                            IterateOverInsns(bcmv, className, builtin, library, nativeHelpers, mi.inlineExpansionInsns, preamble + "    ");
                        }
                    } else if (isBuiltinInstanceMethod(bcmv, insn)) {
                    }
                } else if (isCompilerLibrary(mi.owner)) {
                    if (isLibraryStaticMethod(bcmv, insn)) {
                        ByteCodeVisitor bcv = (ByteCodeVisitor) library.classes.get(mi.owner + ".class");
                        ByteCodeMethodVisitor methodToInline = (ByteCodeMethodVisitor) bcv.methodVisitors.get(mi._name + 
                                                                                                              mi.desc); 
                        if (methodToInline != null && isNotInParentSet(mi)) {
                            inlineMethodCall(className, bcmv, mi, methodToInline,true);
                            IterateOverInsns(bcmv, className, builtin, library, nativeHelpers, mi.inlineExpansionInsns, preamble + "    ");
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
                        IterateOverInsns(bcmv, className, builtin, library, nativeHelpers, mi.inlineExpansionInsns, preamble + "    ");
                    }

                } else if (isUnboxedNativeInterfaceMethod(mi.owner)) {
                    String key = "native/" + mi.owner + ".class";
                    ByteCodeVisitor bcv = (ByteCodeVisitor) nativeHelpers.classes.get(key);
                    ByteCodeMethodVisitor methodToInline = (ByteCodeMethodVisitor) bcv.methodVisitors.get(mi._name + 
                                                                                                          mi.desc); 
                    if (methodToInline != null) {
                        inlineMethodCall(className, bcmv, mi, methodToInline, true);
                        IterateOverInsns(bcmv, className, builtin, library, nativeHelpers, mi.inlineExpansionInsns, preamble + "    ");
                    }
                }
            }
        }
    }
}
