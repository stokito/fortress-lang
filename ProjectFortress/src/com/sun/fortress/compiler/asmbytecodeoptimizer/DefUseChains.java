/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/


package com.sun.fortress.compiler.asmbytecodeoptimizer;

import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public class DefUseChains {

    private static boolean noisy = false;
    ByteCodeMethodVisitor bcmv;

    public static void optimize(ByteCodeVisitor bcv) {
        boolean changed = false;
        Iterator it = bcv.methodVisitors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            ByteCodeMethodVisitor bcmv = (ByteCodeMethodVisitor) pairs.getValue();
            String className = (String) pairs.getKey();
            if (noisy) {
                printMethod(bcmv, "Annotated");
                System.out.println("Printing Values HERE");
                printValues(bcmv);
                printChains(bcmv);
            }
            changed = renameValues(bcmv);
             if (noisy && changed) {
                 printMethod(bcmv, "After Rename");
                 System.out.println("Printing Values After Rename HERE");
                 printValues(bcmv);
                 printChains(bcmv);
              }
             changed = identifyPotentialUnusedBoxedValues(bcmv);
             if (noisy && changed) {
                  System.out.println("After identifying unused vars");
                  printMethod(bcmv, "After Identify Unused");
                  printValues(bcmv);
                  printChains(bcmv);
              }
             // changed = removeUnusedBoxedValues(bcmv);
             // if (noisy && changed) {
             //     System.out.println("After removing unused vars");
             //     printMethod(bcmv, "After RemoveUnused");
             //     printValues(bcmv);
             //     printChains(bcmv);                 
             // }
             // changed = renumberInsns(bcmv);
             // if (noisy && changed) {
             //     printMethod(bcmv, "After Renumber");
             //     System.out.println("Printing Values After Renumber HERE");
             //     printValues(bcmv);
             // }
        }
    }

    private static void printMethod(ByteCodeMethodVisitor method, String when) {
        System.out.println("Printing Method: " + method.name + "  " + when + " with maxStack = " + method.maxStack + " and maxLocals = " + method.maxLocals);
        printInsns(method.insns, when);
        System.out.println("End Method: " + method.name);
    }

    private static void printInsns(List<Insn> insns, String header) {
        for (Insn inst : insns) {
            if (inst.isExpanded()) {
                System.out.println(header + "Replaced Bytecode: " + inst);
                printInsns(inst.inlineExpansionInsns, header + " ");
            } else {
                System.out.println(header + "Bytecode: " + inst + " defs = " + inst.getDefs() + " uses = " + inst.getUses() + " stackValues = " + inst.getStackString() + " localValues = " + inst.getLocals()); 
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

    private static void printValues(ByteCodeMethodVisitor bcmv) {
        System.out.println("Printing Values for method " + bcmv.name);
        for (AbstractInterpretationValue val : bcmv.getValues()) {
            System.out.println(val);
        }
    }

    private static void printChainLine(String prelude, String result) {
        int header = 40;
        String padding = new String("                                                                                                   ");
        String head = padding.substring(0, header - prelude.length());
        System.out.println(prelude + head + result);
    }

    private static void printChains(ByteCodeMethodVisitor bcmv) {
        int counter = 0;
        System.out.println("Printing Chains for " + bcmv);
        for (Insn i : bcmv.insns) {
            String result = "";
            for (AbstractInterpretationValue val : bcmv.getValues()) {
                if (i.defs.contains(val))
                    result = result + "Defines " + val.getValueNumber() + ":" ;
                else if (i.uses.contains(val))
                    result = result + " Uses " + val.getValueNumber() + ":" ;
            }

            //            printChainLine(counter++ + ":" + i + "\n", result);
            System.out.println(counter++ + ":" + i + "\n" + result);
        }
    }

    private static boolean removeUnusedBoxedValues(ByteCodeMethodVisitor bcmv) {
        boolean changed = false;
        for (AbstractInterpretationValue val : bcmv.getValues()) {
            if (val != null && val.isBoxed() && val.notNeeded()) {
                boolean used = false;
                for (Insn i : val.getUses()) {
                    if (i instanceof VarInsn) {
                        if (i.hasMultipleUses()) {
                            boolean needed = false;
                            for (AbstractInterpretationValue v : i.getUses())
                                if (v.isNeeded()) needed = true;
                            used = needed;
                        }
                        // do Nothing
                    } else if (i.isCheckCast()) {
                        // do Nothing
                    } else if (i.isBoxingMethod()) {
                        throw new RuntimeException("Should not get here");
                        // shouldn't get here
                    } else if (i.isUnBoxingMethod()) {
                        if (i.hasDef()) {
                            boolean needed = false;
                            for (AbstractInterpretationValue v : i.getDefs())
                                if (v.isNeeded()) needed = true;
                            // If this unboxing method defines a new value that
                            // we haven't seen before we can't remove it.
                            used = needed;
                        }
                    } else {
                        used = true;
                    }
                }
                if (!used) {
                    removeUnboxedValueInsns(val, bcmv);
                    changed = true;
                }
            }
        }
        return changed;
    }  

    private static void removeInsn(ByteCodeMethodVisitor bcmv, Insn i, AbstractInterpretationValue val, String reason) {
        bcmv.insns.remove(i);
    }


    private static void removeUnboxedValueInsns(AbstractInterpretationValue val, ByteCodeMethodVisitor bcmv) {
        for (Insn i : val.getDefs()) 
            removeInsn(bcmv,i, val, "RemovingBoxedValueDefinition");
        
        for (Insn i : val.getUses()) {
            if (i.isBoxingMethod()) {
                removeInsn(bcmv, i, val, "RemoveBoxingMethod");
            } else if (i.isUnBoxingMethod()) {
                removeInsn(bcmv, i, val, "UnboxingMethod");
            } else if (i.isCheckCast()) {
                removeInsn(bcmv, i, val, "CheckCast"); // FIXME CHF
            } else if (i instanceof VarInsn) {
                VarInsn vi = (VarInsn) i;
                if (vi.opcode == Opcodes.ASTORE) {
                    int j = bcmv.insns.indexOf(i);
                    removeInsn(bcmv, i, val, "astoreconversion" + val.getType());
                    if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FZZ32;"))
                        bcmv.insns.add(j, new VarInsn("ISTORE", Opcodes.ISTORE, val.getValueNumber(), vi.index));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FZZ64;"))
                        bcmv.insns.add(j, new VarInsn("LSTORE", Opcodes.LSTORE, val.getValueNumber(), vi.index));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FRR32;"))
                        bcmv.insns.add(j, new VarInsn("FSTORE", Opcodes.FSTORE, val.getValueNumber(), vi.index));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FRR64;"))
                        bcmv.insns.add(j, new VarInsn("DSTORE", Opcodes.DSTORE, val.getValueNumber(), vi.index));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FBoolean;"))
                        bcmv.insns.add(j, new VarInsn("ISTORE", Opcodes.ISTORE, val.getValueNumber(), vi.index));
                    else bcmv.insns.add(j, new VarInsn("ASTORE", Opcodes.ASTORE, val.getValueNumber(), vi.index));
                } else if (vi.opcode == Opcodes.ALOAD) {
                    int j = bcmv.insns.indexOf(i);
                    removeInsn(bcmv, i, val, "Aloadconversion" + val.getType());
                    if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FZZ32;"))
                        bcmv.insns.add(j, new VarInsn("ILOAD", Opcodes.ILOAD, val.getValueNumber(), vi.index));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FZZ64;"))
                        bcmv.insns.add(j, new VarInsn("LLOAD", Opcodes.LLOAD, val.getValueNumber(), vi.index));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FRR32;"))
                        bcmv.insns.add(j, new VarInsn("FLOAD", Opcodes.FLOAD, val.getValueNumber(), vi.index));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FRR64;"))
                        bcmv.insns.add(j, new VarInsn("DLOAD", Opcodes.DLOAD, val.getValueNumber(), vi.index));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FBoolean;"))
                        bcmv.insns.add(j, new VarInsn("ILOAD", Opcodes.ILOAD, val.getValueNumber(), vi.index));
                    else bcmv.insns.add(j, new VarInsn("ALOAD", Opcodes.ALOAD, val.getValueNumber(), vi.index));
                }
            } else if (i instanceof SingleInsn) {
                SingleInsn si = (SingleInsn) i;
                if (si.opcode == Opcodes.ARETURN) {
                    int j = bcmv.insns.indexOf(i);
                    if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FZZ32;"))
                        bcmv.insns.add(j, new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, 
                                                           "com/sun/fortress/compiler/runtimeValues/FZZ32",
                                                           "make",
                                                           "(I)Lcom/sun/fortress/compiler/runtimeValues/FZZ32;",
                                                           "ReboxingReturnValue"));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FZZ64;"))
                        bcmv.insns.add(j, new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, 
                                                         "com/sun/fortress/compiler/runtimeValues/FZZ64",
                                                         "make",
                                                         "(J)Lcom/sun/fortress/compiler/runtimeValues/FZZ64;",
                                                         "ReboxingReturnValue"));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FRR32;"))
                        bcmv.insns.add(j, new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, 
                                                         "com/sun/fortress/compiler/runtimeValues/FRR32",
                                                         "make",
                                                         "(F)Lcom/sun/fortress/compiler/runtimeValues/FRR32;",
                                                         "ReboxingReturnValue"));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FRR64;"))
                        bcmv.insns.add(j, new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, 
                                                           "com/sun/fortress/compiler/runtimeValues/FRR64",
                                                           "make",
                                                           "(D)Lcom/sun/fortress/compiler/runtimeValues/FRR64;",
                                                           "ReboxingReturnValue"));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FVoid;"))
                        bcmv.insns.add(j, new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, 
                                                         "com/sun/fortress/compiler/runtimeValues/FVoid",
                                                         "make",
                                                         "()Lcom/sun/fortress/compiler/runtimeValues/FVoid;",
                                                         "ReboxingReturnValue"));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FBoolean;"))
                        bcmv.insns.add(j, new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, 
                                                         "com/sun/fortress/compiler/runtimeValues/FBoolean",
                                                         "make",
                                                         "(Z)Lcom/sun/fortress/compiler/runtimeValues/FBoolean;",
                                                         "ReboxingReturnValue"));
                    else if (val.getType().equals("Lcom/sun/fortress/compiler/runtimeValues/FJavaString;"))
                        bcmv.insns.add(j, new MethodInsn("INVOKESTATIC", Opcodes.INVOKESTATIC, 
                                                         "com/sun/fortress/compiler/runtimeValues/FJavaString",
                                                         "make",
                                                         "(java.lang.String)Lcom/sun/fortress/compiler/runtimeValues/FJavaString;",
                                                         "ReboxingReturnValue"));
                    else throw new RuntimeException("Don't recognize var type " + val.getType());
                }
            }
        }
    }

    private static boolean identifyPotentialUnusedBoxedValues (ByteCodeMethodVisitor bcmv) {
        boolean changed = false;
        for (AbstractInterpretationValue val : bcmv.getValues()) {
            if (val != null && val.isBoxed()) {
                boolean used = false;
                for (Insn i : val.getUses()) {
                    if ((i instanceof VarInsn) || i.isUnnecessaryCheckCast(val) || i.isUnBoxingMethod()) {
                        // do nothing
                    } else {
                        used = true;
                    }
                }
                if (!used) {
                    System.out.println("We are setting val: " + val + " as unused uses = " + val.getUses());
                    val.setUnNeeded();
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean renumberInsns(ByteCodeMethodVisitor bcmv) {
        for (int i = 0; i < bcmv.insns.size(); i++)
            bcmv.insns.get(i).setNewIndex(i);
        return true;
    }

    private static boolean renameValues(ByteCodeMethodVisitor bcmv) {
        boolean changed = false;
        HashMap replacements = new HashMap();
        
        for (AbstractInterpretationValue val : bcmv.getValues()) {
            HashSet<Insn> UpdatedValUses = new HashSet<Insn>();

            for (Insn i : val.getUses()) {
                if (i instanceof VarInsn) {
                    VarInsn vi = (VarInsn) i;
                    int j = bcmv.insns.indexOf(i);
                    if (j > 0) {
                        Insn k = new VarInsn(vi.name, vi.opcode, val.getValueNumber(), vi.index);
                        Insn old = (Insn) replacements.put(vi, k);
                        UpdatedValUses.add(k);
                        changed = true;
                    }
                } else {
                    UpdatedValUses.add(i);
                }
            }
            val.setUses(UpdatedValUses);                
        }

        for (Object o : replacements.keySet()) {
            Insn i = (Insn) o;
            int j = bcmv.insns.indexOf(i);
            Insn k = (Insn) replacements.get(i);
            if (j >= 0) {
                bcmv.insns.remove(j);
                bcmv.insns.add(j, k);
            }
        }
    return changed;
    }
}

