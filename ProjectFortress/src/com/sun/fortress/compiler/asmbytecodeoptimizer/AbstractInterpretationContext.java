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

public class AbstractInterpretationContext {
    AbstractInterpretation ai;
    ByteCodeMethodVisitor bcmv;
    Object stack[];
    Object locals[];
    int stackIndex;
    int pc;  

    void pushStack(Object o) {stack[stackIndex++] = o;}
    Object popStack() {return stack[--stackIndex];}

    


    AbstractInterpretationContext(AbstractInterpretation ai,
                                  ByteCodeMethodVisitor bcmv, Object stack[], 
                                  Object locals[], int stackIndex, int pc) {
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
        System.out.println("interpretMethod: pc = " + pc);
        while (pc < bcmv.insns.size()) {
            interpretInsn(bcmv.insns.get(pc++));
        }
    }

    public void interpretInsn(Insn i) {
        i.setStack(stack);
        i.setLocals(locals);
        System.out.println("InterpretInsn: " + i + getStackString() + getLocalsString());

        if (i instanceof FieldInsn) { interpretFieldInsn((FieldInsn) i); }
        else if (i instanceof IincInsn)  { interpretIincInsn((IincInsn) i);}
        else if (i instanceof IntInsn)   { interpretIntInsn((IntInsn) i);}
        else if (i instanceof JumpInsn)  { interpretJumpInsn((JumpInsn) i);}
        else if (i instanceof LabelInsn) { interpretLabelInsn((LabelInsn) i);}
        else if (i instanceof LdcInsn)   { interpretLdcInsn((LdcInsn) i);}
        else if (i instanceof LookupSwitchInsn) { 
            interpretLookupSwitchInsn((LookupSwitchInsn) i);
        }
        else if (i instanceof MethodInsn) { interpretMethodInsn((MethodInsn) i);}
        else if (i instanceof NotYetImplementedInsn) {}
        else if (i instanceof SingleInsn)      { interpretSingleInsn((SingleInsn) i);}
        else if (i instanceof TableSwitchInsn) { interpretTableSwitchInsn((TableSwitchInsn) i);}
        else if (i instanceof TypeInsn) { interpretTypeInsn((TypeInsn) i);}
        else if (i instanceof VarInsn) { interpretVarInsn((VarInsn) i);}
        else if (i instanceof VisitLineNumberInsn) { 
            interpretVisitLineNumberInsn((VisitLineNumberInsn) i);}
        else if (i instanceof VisitMaxs) {}
        else if (i instanceof VisitEnd) {}
        else if (i instanceof VisitFrame) {}
        else if (i instanceof LocalVariable) {}
        else NYI(i);
        
    }


    void interpretFieldInsn(FieldInsn i) {
        int opcode = i.opcode;
        if (opcode == Opcodes.GETFIELD) {
            popStack();
            pushStack(i.desc);
        } else if (opcode == Opcodes.GETSTATIC) {
            pushStack(i.desc);
        } else if (opcode == Opcodes.PUTSTATIC) {
            popStack();
        } else if (opcode == Opcodes.PUTFIELD) {
            popStack();
            popStack();
        }
        else throw new RuntimeException("Unknown field instruction");
    }

    void interpretVarInsn(VarInsn i) {
        int opcode = i.opcode;
        if (opcode == Opcodes.ILOAD)
            pushStack("I");
        else if (opcode == Opcodes.LLOAD)
            pushStack("J");
        else if (opcode == Opcodes.FLOAD)
            pushStack("F");
        else if (opcode == Opcodes.DLOAD)
            pushStack("D");
        else if (opcode == Opcodes.ALOAD)
            pushStack(locals[i.var]);
        else if (opcode == Opcodes.ISTORE)
            locals[i.var] = "I";
        else if (opcode == Opcodes.LSTORE)
            locals[i.var] = "J";
        else if (opcode == Opcodes.FSTORE)
            locals[i.var] = "F";
        else if (opcode == Opcodes.DSTORE)
            locals[i.var] = "D";
        else if (opcode == Opcodes.ASTORE)
            locals[i.var] = popStack();
        else throw new RuntimeException("Unknown field instruction");
    }

    void interpretMethodInsn(MethodInsn i) {
        int opcode = i.opcode;
        if (opcode == Opcodes.INVOKEVIRTUAL || 
            opcode == Opcodes.INVOKEINTERFACE ||
            opcode == Opcodes.INVOKESPECIAL) {
            List<String> args = NamingCzar.parseArgs(i.desc);
            for (int j = 0; j < args.size(); j++)
                popStack();
            popStack(); // owner
            String result = NamingCzar.parseResult(i.desc);
            System.out.println("result = " + result);
            if (result.compareTo("V") != 0)
                pushStack(result);
        } else if (opcode == Opcodes.INVOKESTATIC ) {
            List<String> args = NamingCzar.parseArgs(i.desc);
            for (int j = 0; j < args.size(); j++)
                popStack();
            String result = NamingCzar.parseResult(i.desc);
            System.out.println("result = " + result);
            if (result.compareTo("V") != 0)
                pushStack(result);
        } else { System.out.println("Don't know how to interpret methodInsn " + i.toString()); }
    }

    void interpretIincInsn(IincInsn i) {}
    void interpretIntInsn(IntInsn i) {}

    void addNext(JumpInsn i) {
            Integer nextInsns = (Integer) bcmv.labelNames.get(i.label.toString());
            AbstractInterpretationContext next = 
                new AbstractInterpretationContext(ai, bcmv, stack, locals, stackIndex, nextInsns.intValue());
            ai.instructions.add(next);
    }

    void interpretJumpInsn(JumpInsn i) {
        switch(i.opcode) {
        case Opcodes.IFEQ: 
        case Opcodes.IFNE: 
        case Opcodes.IFLT: 
        case Opcodes.IFGE: 
        case Opcodes.IFGT: 
        case Opcodes.IFLE: popStack(); addNext(i); break;
        case Opcodes.IF_ICMPEQ:
        case Opcodes.IF_ICMPNE:
        case Opcodes.IF_ICMPLT:
        case Opcodes.IF_ICMPGE:
        case Opcodes.IF_ICMPGT:
        case Opcodes.IF_ICMPLE:
        case Opcodes.IF_ACMPEQ:
        case Opcodes.IF_ACMPNE: NYI(i); break;
        case Opcodes.GOTO: {
            Integer next = (Integer) bcmv.labelNames.get(i.label.toString());
            pc = next.intValue();
            break;
        }
        case Opcodes.JSR: NYI(i); break;
        case Opcodes.IFNULL: NYI(i); break;
        case Opcodes.IFNONNULL: NYI(i); break;
        default: NYI(i); 
        }
    }
    void interpretLabelInsn(LabelInsn i) {}
    void interpretLdcInsn(LdcInsn i) {
        pushStack(i.cst);
    }
    void interpretLookupSwitchInsn(LookupSwitchInsn i) {}
 
   void interpretSingleInsn(SingleInsn i) {
        switch(i.opcode) {
        case Opcodes.NOP: break;
        case Opcodes. ACONST_NULL: pushStack("Null"); break;
        case Opcodes.ICONST_M1: 
        case Opcodes.ICONST_0: 
        case Opcodes.ICONST_1: 
        case Opcodes.ICONST_2: 
        case Opcodes.ICONST_3: 
        case Opcodes.ICONST_4: 
        case Opcodes.ICONST_5: pushStack("I"); break;
        case Opcodes.LCONST_0: 
        case Opcodes.LCONST_1: pushStack("J"); break;
        case Opcodes.FCONST_0: 
        case Opcodes.FCONST_1: 
        case Opcodes.FCONST_2: pushStack("F"); break;
        case Opcodes.DCONST_0: 
        case Opcodes.DCONST_1: pushStack("D"); break;
        case Opcodes.IALOAD: pushStack("I"); break;
        case Opcodes.LALOAD: pushStack("J"); break;
        case Opcodes.FALOAD: pushStack("F"); break;
        case Opcodes.DALOAD: pushStack("D"); break;
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
        case Opcodes.POP: popStack(); break; 
        case Opcodes.POP2: NYI(i); break; 
        case Opcodes.DUP: Object x = popStack(); pushStack(x); pushStack(x); break;
        case Opcodes.DUP_X1: NYI(i); break; 
        case Opcodes.DUP_X2: NYI(i); break; 
        case Opcodes.DUP2: NYI(i); break; 
        case Opcodes.DUP2_X1: NYI(i); break; 
        case Opcodes.DUP2_X2: NYI(i); break; 
        case Opcodes.SWAP: NYI(i); break; 
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
        case Opcodes.IRETURN: popStack(); pc = bcmv.insns.size(); break; 
        case Opcodes.LRETURN: popStack(); popStack(); pc = bcmv.insns.size(); break; 
        case Opcodes.FRETURN: popStack(); pc = bcmv.insns.size(); break; 
        case Opcodes.DRETURN: popStack(); popStack(); pc = bcmv.insns.size(); break; 
        case Opcodes.ARETURN: popStack(); pc = bcmv.insns.size(); break; 
        case Opcodes.RETURN:  pc = bcmv.insns.size(); break; 
        case Opcodes.ARRAYLENGTH: NYI(i); break; 
        case Opcodes.ATHROW: popStack(); break; 
        case Opcodes.MONITORENTER: NYI(i); break; 
        case Opcodes.MONITOREXIT: NYI(i); break;
        default: 
            NYI(i);
        }
    }
    void interpretTableSwitchInsn(TableSwitchInsn i) {NYI(i);}
    void interpretTypeInsn(TypeInsn i) {
        switch (i.opcode) {
        case Opcodes.NEW: pushStack(i.type); break;
        case Opcodes.ANEWARRAY: NYI(i); break;
        case Opcodes.CHECKCAST:  popStack(); pushStack(i.type); break;
        case Opcodes.INSTANCEOF: popStack(); pushStack("I"); break;
        default: NYI(i); break;
        }
    }

    void interpretVisitLineNumberInsn(VisitLineNumberInsn i) {}

}