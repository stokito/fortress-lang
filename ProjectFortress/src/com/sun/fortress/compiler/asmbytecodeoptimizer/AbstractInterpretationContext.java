/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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


public class AbstractInterpretationContext {
    AbstractInterpretation ai;
    ByteCodeMethodVisitor bcmv;
    AbstractInterpretationValue stack[];
    AbstractInterpretationValue locals[];
    Object defs[];
    Object uses[];
    int stackIndex;
    int pc;
    private final static boolean noisy = false;

    private int defCount = 0;

    // 
    void pushStackDefinition(Insn i, AbstractInterpretationValue s) {
        i.addDef(s);
        stack[stackIndex++] = s;
    }

    void pushStack(Insn i, AbstractInterpretationValue s) {
        s.addUse(i);
        stack[stackIndex++] = s;
    }

    AbstractInterpretationValue popStack(Insn i) { 
        AbstractInterpretationValue result = stack[--stackIndex];
        result.addUse(i);
        return result;
    }

    AbstractInterpretationContext(AbstractInterpretation ai,
                                  ByteCodeMethodVisitor bcmv, 
                                  AbstractInterpretationValue stack[], 
                                  AbstractInterpretationValue locals[], 
                                  int stackIndex, int pc) {
        this.ai = ai;
        this.bcmv = bcmv;
        this.stack = stack;
        this.locals = locals;
        this.stackIndex = stackIndex;
        this.pc = pc;
    }

    public String getStackString() {
        String result = "[";
        for(int i = 0; i < stackIndex; i++) 
            result = result + " " + stack[i];
        result = result + "]";
        return result;
    }

    public String getLocalsString() {
        String result = "{";
        for (int i=0; i < locals.length; i++)
            result = result + " " + locals[i];
        result = result + "}";
        return result;
    }

    public void NYI(Insn i) {
        throw new RuntimeException("Abstract Interpretation: Insn " + i + " not yet implemented");
    }

     public void interpretMethod() {
         if (noisy) System.out.println("interpretMethod: pc = " + pc);
         while (pc < bcmv.insns.size()) {
             interpretInsn(bcmv.insns.get(pc), pc++);
         }
         bcmv.sortValues();
     }

    void printInsn(Insn i, int pc) {
        if (noisy)  {
            System.out.println("InterpretInsn: pc= " + pc + " insn = " + i + getStackString() + getLocalsString());
        }
    }        

    public void interpretInsn(Insn i, int pc) {
        i.setStack(stack);
        i.setLocals(locals);

        if (i instanceof FieldInsn)      { printInsn(i, pc); interpretFieldInsn((FieldInsn) i); }
        else if (i instanceof IincInsn)  { printInsn(i, pc); interpretIincInsn((IincInsn) i);}
        else if (i instanceof IntInsn)   { printInsn(i, pc); interpretIntInsn((IntInsn) i);}
        else if (i instanceof JumpInsn)  { printInsn(i, pc); interpretJumpInsn((JumpInsn) i);}
        else if (i instanceof LabelInsn) { interpretLabelInsn((LabelInsn) i);}
        else if (i instanceof LdcInsn)   { printInsn(i, pc); interpretLdcInsn((LdcInsn) i);}
        else if (i instanceof LookupSwitchInsn) { 
            printInsn(i, pc); 
            interpretLookupSwitchInsn((LookupSwitchInsn) i);
        }
        else if (i instanceof MethodInsn) { 
            printInsn(i, pc); 
            interpretMethodInsn((MethodInsn) i);
        }
        else if (i instanceof NotYetImplementedInsn) { printInsn(i, pc); throw new RuntimeException("NYI"); }
        else if (i instanceof SingleInsn)     { printInsn(i, pc); interpretSingleInsn((SingleInsn) i);}
        else if (i instanceof TableSwitchInsn) { printInsn(i, pc); interpretTableSwitchInsn((TableSwitchInsn) i);}
        else if (i instanceof TypeInsn) { printInsn(i, pc);  interpretTypeInsn((TypeInsn) i);}
        else if (i instanceof VarInsn) { printInsn(i, pc); interpretVarInsn((VarInsn) i);}
        else if (i instanceof VisitLineNumberInsn) { 
            interpretVisitLineNumberInsn((VisitLineNumberInsn) i);}
        else if (i instanceof VisitMaxs) {}
        else if (i instanceof VisitCode) {}
        else if (i instanceof VisitEnd) {}
        else if (i instanceof VisitFrame) {}
        else if (i instanceof LocalVariableInsn) {}
        else NYI(i);
    }


    void interpretFieldInsn(FieldInsn i) {
        int opcode = i.opcode;
        if (opcode == Opcodes.GETFIELD) {
            popStack(i);
            pushStackDefinition(i, bcmv.createValue(i, i.desc));
        } else if (opcode == Opcodes.GETSTATIC) {
            pushStackDefinition(i, bcmv.createValue(i, i.desc));
        } else if (opcode == Opcodes.PUTSTATIC) {
            popStack(i);
        } else if (opcode == Opcodes.PUTFIELD) {
            popStack(i);
            popStack(i);
        }
        else throw new RuntimeException("Unknown field instruction");
    }

    void interpretLoad(VarInsn i, int opcode, String expected, AbstractInterpretationValue val) {
        if (!val.getType().startsWith(expected))
            System.out.println(" Op " + Opcodes.opcNames[opcode] + " expected " + expected + " but got " + val.getType());            
        pushStack(i, val);
    }

    void interpretStore(VarInsn i, int opcode, String expected) {
        AbstractInterpretationValue val = popStack(i);
        if (!val.getType().startsWith(expected)) 
            System.out.println(" Op " + Opcodes.opcNames[opcode] + " expected " + expected + " but got " + val.getType());            
        locals[i.var] = val;
    }
            

    void interpretVarInsn(VarInsn i) {
        int opcode = i.opcode;
        AbstractInterpretationValue val = locals[i.var];
        switch(opcode) {
        case Opcodes.ILOAD:  interpretLoad(i, opcode, "I", val); break;
        case Opcodes.LLOAD:  interpretLoad(i, opcode, "J", val); break;
        case Opcodes.FLOAD:  interpretLoad(i, opcode, "F", val); break;
        case Opcodes.DLOAD:  interpretLoad(i, opcode, "D", val); break;
        case Opcodes.ALOAD:  interpretLoad(i, opcode, "L", val); break;
        case Opcodes.ISTORE: interpretStore(i, opcode, "I");     break;
        case Opcodes.LSTORE: interpretStore(i, opcode, "J");     break;
        case Opcodes.FSTORE: interpretStore(i, opcode, "F");     break;
        case Opcodes.DSTORE: interpretStore(i, opcode, "D");     break;
        case Opcodes.ASTORE: interpretStore(i, opcode, "L");     break;
        default: throw new RuntimeException("Unknown VarInsn Opcode= " + opcode);
        }
    }

    void interpretBoxingMethodInsn(MethodInsn mi) {
        String result = NamingCzar.parseResult(mi.desc);
        if (mi.isFVoidBoxingMethod()) {
            pushStackDefinition(mi, bcmv.createValue(mi, result));
        } else {
            AbstractInterpretationValue val = popStack(mi);
            pushStackDefinition(mi, bcmv.createValue(mi, result, val));
        }
    }
        
    void interpretUnBoxingMethodInsn(MethodInsn mi) {
        String result = NamingCzar.parseResult(mi.desc);
        List<String> args = NamingCzar.parseArgs(mi.desc);
        int k = args.size() - 1;

         if (args.size() > 0)
            throw new RuntimeException("Called Unboxing method with one or more arguements " + mi + " " + args.size());

        AbstractInterpretationValue val = popStack(mi);
        if (val instanceof AbstractInterpretationBoxedValue) {
            AbstractInterpretationBoxedValue bv = (AbstractInterpretationBoxedValue) val;
            if (!result.equals(bv.unboxed().getType()))
                throw new RuntimeException("The unboxed value with type " + bv.unboxed().getType() + " should have the expected type " + result);
            pushStack(mi, bv.unboxed());
        } else {
         if (result.compareTo("V") != 0) {
             pushStackDefinition(mi, bcmv.createValue(mi, result));
         }
        }
    }

    void interpretStaticMethodInsn(MethodInsn mi) {
         int opcode = mi.opcode;
         String result = NamingCzar.parseResult(mi.desc);
         List<String> args = NamingCzar.parseArgs(mi.desc);
         int k = args.size() - 1;

         for (int i = 0; i < args.size(); i++) {
             popStack(mi);
         }

         if (result.compareTo("V") != 0) {
             pushStackDefinition(mi, bcmv.createValue(mi, result));
         }
    }

    void interpretInterfaceMethodInsn(MethodInsn mi) {
         int opcode = mi.opcode;
         String result = NamingCzar.parseResult(mi.desc);
         List<String> args = NamingCzar.parseArgs(mi.desc);

         for (int i = 0; i < args.size(); i++) {
             popStack(mi);
         }
         popStack(mi); //owner

         if (result.compareTo("V") != 0) {
             pushStackDefinition(mi, bcmv.createValue(mi, result));
         }
    }
        

    void interpretNonStaticMethodInsn(MethodInsn mi) {
         int opcode = mi.opcode;
         String result = NamingCzar.parseResult(mi.desc);
         List<String> args = NamingCzar.parseArgs(mi.desc);

         for (int i = 0; i < args.size(); i++) {
             popStack(mi);
         }
         popStack(mi); //owner

         if (result.compareTo("V") != 0) {
             pushStackDefinition(mi, bcmv.createValue(mi, result));
         }
    }

     void interpretMethodInsn(MethodInsn mi) {
         if (mi.isBoxingMethod()) 
             interpretBoxingMethodInsn(mi);
         else if (mi.isUnBoxingMethod())
             interpretUnBoxingMethodInsn(mi);
         else if (mi.isStaticMethod()) 
             interpretStaticMethodInsn(mi);
         else if (mi.isInterfaceMethod())
             interpretInterfaceMethodInsn(mi);
         else interpretNonStaticMethodInsn(mi);
     }

    void interpretIincInsn(IincInsn i) {}
    void interpretIntInsn(IntInsn i) {}

    int getNext(ByteCodeMethodVisitor bcmv, JumpInsn i) {

        Integer loc = (Integer) bcmv.labelDefs.get(i.label.toString());
        return loc.intValue();
    }

    void addNext(JumpInsn i) {
        AbstractInterpretationContext next = 
            new AbstractInterpretationContext(ai, bcmv, stack, locals, 
                                              stackIndex, getNext(bcmv, i));
        ai.instructions.add(next);
    }

    void interpretJumpInsn(JumpInsn i) {
        switch(i.opcode) {
        case Opcodes.IFEQ: 
        case Opcodes.IFNE: 
        case Opcodes.IFLT: 
        case Opcodes.IFGE: 
        case Opcodes.IFGT: 
        case Opcodes.IFLE: {
            popStack(i);
            addNext(i);
            break;
        }
        case Opcodes.IF_ICMPEQ:
        case Opcodes.IF_ICMPNE:
        case Opcodes.IF_ICMPLT:
        case Opcodes.IF_ICMPGE:
        case Opcodes.IF_ICMPGT:
        case Opcodes.IF_ICMPLE:
        case Opcodes.IF_ACMPEQ:
        case Opcodes.IF_ACMPNE: {
            popStack(i);
            popStack(i);
            addNext(i);
            break;
        }
        case Opcodes.GOTO: {
            pc = getNext(bcmv,i);
            break;
        }
        case Opcodes.JSR: NYI(i); break;
        case Opcodes.IFNULL: 
        case Opcodes.IFNONNULL:  {
            popStack(i);
            addNext(i);
            break;
        }

        default: NYI(i); 
        }
    }
    void interpretLabelInsn(LabelInsn i) {}
    void interpretLdcInsn(LdcInsn i) {
        Object o = i.cst;

        if (o instanceof Integer) pushStackDefinition(i, bcmv.createValue(i, "I"));
        else if (o instanceof Long) pushStackDefinition(i, bcmv.createValue(i, "J"));
        else if (o instanceof Float) pushStackDefinition(i, bcmv.createValue(i, "F"));
        else if (o instanceof Double) pushStackDefinition(i, bcmv.createValue(i, "D"));
        else pushStackDefinition(i, bcmv.createValue(i, "L"+ o.getClass().getName().replace(".", "/") + ";"));
    }

    void interpretLookupSwitchInsn(LookupSwitchInsn i) {}
 
   void interpretSingleInsn(SingleInsn i) {
        switch(i.opcode) {
        case Opcodes.NOP: break;
        case Opcodes. ACONST_NULL: pushStackDefinition(i, bcmv.createValue(i, "NULL")); break;
        case Opcodes.ICONST_M1: 
        case Opcodes.ICONST_0: 
        case Opcodes.ICONST_1: 
        case Opcodes.ICONST_2: 
        case Opcodes.ICONST_3: 
        case Opcodes.ICONST_4: 
        case Opcodes.ICONST_5: pushStackDefinition(i, bcmv.createValue(i, "I"));break;
        case Opcodes.LCONST_0: 
        case Opcodes.LCONST_1: pushStackDefinition(i, bcmv.createValue(i, "J"));break;
        case Opcodes.FCONST_0: 
        case Opcodes.FCONST_1: 
        case Opcodes.FCONST_2: pushStackDefinition(i, bcmv.createValue(i, "F")); break;
        case Opcodes.DCONST_0: 
        case Opcodes.DCONST_1: pushStackDefinition(i, bcmv.createValue(i, "D")); break;
        case Opcodes.IALOAD:   pushStackDefinition(i, bcmv.createValue(i, "I")); break;
        case Opcodes.LALOAD:   pushStackDefinition(i, bcmv.createValue(i, "J")); break;
        case Opcodes.FALOAD:   pushStackDefinition(i, bcmv.createValue(i, "F")); break;
        case Opcodes.DALOAD:   pushStackDefinition(i, bcmv.createValue(i, "D")); break;
        case Opcodes.AALOAD: NYI(i); break; 
        case Opcodes.BALOAD: NYI(i); break; 
        case Opcodes.CALOAD: NYI(i); break; 
        case Opcodes.SALOAD: NYI(i); break; 
        case Opcodes.IASTORE: NYI(i); break; 
        case Opcodes.LASTORE: NYI(i); break; 
        case Opcodes.FASTORE: NYI(i); break; 
        case Opcodes.DASTORE: NYI(i); break; 
        case Opcodes.AASTORE: NYI(i); break; 
        case Opcodes.BASTORE: NYI(i); break; 
        case Opcodes.CASTORE: NYI(i); break; 
        case Opcodes.SASTORE: NYI(i); break; 
        case Opcodes.POP: popStack(i); break; 
        case Opcodes.POP2: NYI(i); break; 
        case Opcodes.DUP: {
            AbstractInterpretationValue dup_x_value = popStack(i); 
            pushStack(i, dup_x_value); 
            pushStack(i, dup_x_value); 
            break;
        }
        case Opcodes.DUP_X1: NYI(i); break; 
        case Opcodes.DUP_X2: NYI(i); break; 
        case Opcodes.DUP2: NYI(i); break; 
        case Opcodes.DUP2_X1: NYI(i); break; 
        case Opcodes.DUP2_X2: NYI(i); break; 
        case Opcodes.SWAP: {
            AbstractInterpretationValue swap_x_value = popStack(i);
            AbstractInterpretationValue swap_y_value = popStack(i);
            pushStack(i, swap_y_value);
            pushStack(i, swap_x_value);
            break;
        }
        case Opcodes.IADD: NYI(i); break; 
        case Opcodes.LADD: NYI(i); break; 
        case Opcodes.FADD: NYI(i); break; 
        case Opcodes.DADD: NYI(i); break; 
        case Opcodes.ISUB: NYI(i); break; 
        case Opcodes.LSUB: NYI(i); break; 
        case Opcodes.FSUB: NYI(i); break; 
        case Opcodes.DSUB: NYI(i); break; 
        case Opcodes.IMUL: NYI(i); break; 
        case Opcodes.LMUL: NYI(i); break; 
        case Opcodes.FMUL: NYI(i); break; 
        case Opcodes.DMUL: NYI(i); break; 
        case Opcodes.IDIV: NYI(i); break; 
        case Opcodes.LDIV: NYI(i); break; 
        case Opcodes.FDIV: NYI(i); break; 
        case Opcodes.DDIV: NYI(i); break; 
        case Opcodes.IREM: NYI(i); break; 
        case Opcodes.LREM: NYI(i); break; 
        case Opcodes.FREM: NYI(i); break; 
        case Opcodes.DREM: NYI(i); break; 
        case Opcodes.INEG: NYI(i); break; 
        case Opcodes.LNEG: NYI(i); break; 
        case Opcodes.FNEG: NYI(i); break; 
        case Opcodes.DNEG: NYI(i); break; 
        case Opcodes.ISHL: NYI(i); break; 
        case Opcodes.LSHL: NYI(i); break; 
        case Opcodes.ISHR: NYI(i); break; 
        case Opcodes.LSHR: NYI(i); break; 
        case Opcodes.IUSHR: NYI(i); break; 
        case Opcodes.LUSHR: NYI(i); break; 
        case Opcodes.IAND: NYI(i); break; 
        case Opcodes.LAND: NYI(i); break; 
        case Opcodes.IOR: NYI(i); break; 
        case Opcodes.LOR: NYI(i); break; 
        case Opcodes.IXOR: NYI(i); break; 
        case Opcodes.LXOR: NYI(i); break; 
        case Opcodes.I2L: NYI(i); break; 
        case Opcodes.I2F: NYI(i); break; 
        case Opcodes.I2D: NYI(i); break; 
        case Opcodes.L2I: NYI(i); break; 
        case Opcodes.L2F: NYI(i); break; 
        case Opcodes.L2D: NYI(i); break; 
        case Opcodes.F2I: NYI(i); break; 
        case Opcodes.F2L: NYI(i); break; 
        case Opcodes.F2D: NYI(i); break; 
        case Opcodes.D2I: NYI(i); break; 
        case Opcodes.D2L: NYI(i); break; 
        case Opcodes.D2F: NYI(i); break; 
        case Opcodes.I2B: NYI(i); break; 
        case Opcodes.I2C: NYI(i); break; 
        case Opcodes.I2S: NYI(i); break; 
        case Opcodes.LCMP: NYI(i); break; 
        case Opcodes.FCMPL: NYI(i); break; 
        case Opcodes.FCMPG: NYI(i); break; 
        case Opcodes.DCMPL: NYI(i); break; 
        case Opcodes.DCMPG: NYI(i); break; 
        case Opcodes.IRETURN: popStack(i); pc = bcmv.insns.size(); break; 
        case Opcodes.LRETURN: popStack(i); popStack(i); pc = bcmv.insns.size(); break; 
        case Opcodes.FRETURN: popStack(i); pc = bcmv.insns.size(); break; 
        case Opcodes.DRETURN: popStack(i); popStack(i); pc = bcmv.insns.size(); break; 
        case Opcodes.ARETURN: popStack(i); pc = bcmv.insns.size(); break; 
        case Opcodes.RETURN:  pc = bcmv.insns.size(); break; 
        case Opcodes.ARRAYLENGTH: NYI(i); break; 
        case Opcodes.ATHROW: popStack(i); pc = bcmv.insns.size(); break; 
        case Opcodes.MONITORENTER: popStack(i); break; 
        case Opcodes.MONITOREXIT: popStack(i); break;
        default: 
            NYI(i);
        }
   }


    void interpretTableSwitchInsn(TableSwitchInsn i) {NYI(i);}

    void interpretTypeInsn(TypeInsn i) {
        switch (i.opcode) {
        case Opcodes.NEW: pushStackDefinition(i, bcmv.createValue(i, i.type)); break;
        case Opcodes.ANEWARRAY: NYI(i); break;
        case Opcodes.CHECKCAST: {
            AbstractInterpretationValue v = popStack(i); 
            pushStack(i, v);
            break;
        }
        case Opcodes.INSTANCEOF: popStack(i); pushStackDefinition(i, bcmv.createValue(i, "I")); break;
        default: NYI(i); break;
        }
    }

    void interpretVisitLineNumberInsn(VisitLineNumberInsn i) {}

    public String toString() {
        return  getStackString() + getLocalsString();
    }
}

