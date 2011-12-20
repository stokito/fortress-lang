/*******************************************************************************
    Copyright 2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import com.sun.fortress.compiler.NamingCzar;
import java.util.List;

public class AbstractInterpretationInsn {

    public static void interpretInsn(AbstractInterpretationContext c, Insn i, int pc) {
        if (i instanceof FieldInsn)       
            interpretFieldInsn(c, (FieldInsn) i); 
        else if (i instanceof IincInsn)   
            interpretIincInsn(c, (IincInsn) i);
        else if (i instanceof IntInsn)    
            interpretIntInsn(c, (IntInsn) i);
        else if (i instanceof JumpInsn)   
            interpretJumpInsn(c, (JumpInsn) i);
        else if (i instanceof LabelInsn)  
            interpretLabelInsn(c, (LabelInsn) i);
        else if (i instanceof LdcInsn)    
            interpretLdcInsn(c, (LdcInsn) i);
        else if (i instanceof LookupSwitchInsn)  
            interpretLookupSwitchInsn(c, (LookupSwitchInsn) i); 
        else if (i instanceof MethodInsn) 
            interpretMethodInsn(c, (MethodInsn) i);
        else if (i instanceof NotYetImplementedInsn) 
            throw new RuntimeException("NYI"); 
        else if (i instanceof SingleInsn) 
            interpretSingleInsn(c, (SingleInsn) i);
        else if (i instanceof TableSwitchInsn) 
            interpretTableSwitchInsn(c, (TableSwitchInsn) i);
        else if (i instanceof TryCatchBlock) 
            interpretTryCatchBlock(c, (TryCatchBlock) i);
        else if (i instanceof TypeInsn) 
            interpretTypeInsn(c, (TypeInsn) i);
        else if (i instanceof VarInsn) 
            interpretVarInsn(c, (VarInsn) i);
        else if (i instanceof VisitLineNumberInsn)
            interpretVisitLineNumberInsn(c, (VisitLineNumberInsn) i);
        else if (i instanceof VisitMaxs) {}
        else if (i instanceof VisitCode) {}
        else if (i instanceof VisitEnd) {}
        else if (i instanceof VisitFrame) {}
        else if (i instanceof LocalVariableInsn) {}
        else NYI(i);

        i.setStack(c.stack);
        i.setLocals(c.locals);
    }

    static void interpretFieldInsn(AbstractInterpretationContext c, FieldInsn i) {
        int opcode = i.opcode;
        if (opcode == Opcodes.GETFIELD) {
            c.popStack(i);
            c.pushStackDefinition(i, i.desc);
        } else if (opcode == Opcodes.GETSTATIC) {
            c.pushStackDefinition(i, i.desc);
        } else if (opcode == Opcodes.PUTSTATIC) {
            c.popStack(i);
        } else if (opcode == Opcodes.PUTFIELD) {
            c.popStack(i);
            c.popStack(i);
        }
        else throw new RuntimeException("Unknown field instruction");
    }

    static void interpretLoad(AbstractInterpretationContext c, VarInsn i) {
        c.pushStack(i, c.getLocal(i.var));
    }

    static void interpretStore(AbstractInterpretationContext c, VarInsn i) {
        AbstractInterpretationValue val = c.popStack(i);
        c.setLocal(i.var, val);
    }

    static void interpretVarInsn(AbstractInterpretationContext c, VarInsn i) {
        int opcode = i.opcode;
        switch(opcode) {
        case Opcodes.ILOAD:
        case Opcodes.LLOAD:  
        case Opcodes.FLOAD:  
        case Opcodes.DLOAD:  
        case Opcodes.ALOAD:  interpretLoad(c, i); break;
        case Opcodes.ISTORE: 
        case Opcodes.LSTORE: 
        case Opcodes.FSTORE: 
        case Opcodes.DSTORE: 
        case Opcodes.ASTORE: interpretStore(c, i); break;
        default: throw new RuntimeException("Unknown VarInsn Opcode= " + opcode);
        }
    }

    static void interpretBoxingMethodInsn(AbstractInterpretationContext c, MethodInsn mi) {
        String result = NamingCzar.parseResult(mi.desc);
        if (mi.isFVoidBoxingMethod()) {
            c.pushStackDefinition(mi, result);
        } else {
            AbstractInterpretationValue val = c.popStack(mi);
            c.pushStackBoxedDefinition(mi, result, val);
        }
    }

    static void interpretUnBoxingMethodInsn(AbstractInterpretationContext c, MethodInsn mi) {
        String result = NamingCzar.parseResult(mi.desc);
        List<String> args = NamingCzar.parseArgs(mi.desc);
        int k = args.size() - 1;

        // This means we are trying to unbox a vector.  
        // We don't know how to do that yet.
        if (args.size() > 0) {
            interpretNonStaticMethodInsn(c,mi);
        }

        AbstractInterpretationValue val = c.popStack(mi);
        if (val instanceof AbstractInterpretationBoxedValue) {
            AbstractInterpretationBoxedValue bv = (AbstractInterpretationBoxedValue) val;
            if (!result.equals(bv.unboxed().getType()))
                throw new RuntimeException("The unboxed value with type " + bv.unboxed().getType() + " should have the expected type " + result);
            c.pushStack(mi, bv.unboxed());
        } else {
            if (result.compareTo("V") != 0) {
                c.pushStackDefinition(mi, result);
            }
        }
    }

    static void interpretStaticMethodInsn(AbstractInterpretationContext c, MethodInsn mi) {
        int opcode = mi.opcode;
        String result = NamingCzar.parseResult(mi.desc);
        List<String> args = NamingCzar.parseArgs(mi.desc);

        for (int i = 0; i < args.size(); i++) {
            c.popStack(mi);
        }

        if (result.compareTo("V") != 0) {
            c.pushStackDefinition(mi, result);
        }
    }

    static void interpretNonStaticMethodInsn(AbstractInterpretationContext c, MethodInsn mi) {
        int opcode = mi.opcode;
        String result = NamingCzar.parseResult(mi.desc);
        List<String> args = NamingCzar.parseArgs(mi.desc);

        for (int i = 0; i < args.size(); i++) {
            c.popStack(mi);
        }
        c.popStack(mi); //owner

        if (result.compareTo("V") != 0) {
            c.pushStackDefinition(mi, result);
        }
    }

    static void interpretMethodInsn(AbstractInterpretationContext c, MethodInsn mi) {
        if (mi.isBoxingMethod()) 
            interpretBoxingMethodInsn(c, mi);
        else if (mi.isUnBoxingMethod())
            interpretUnBoxingMethodInsn(c, mi);
        else if (mi.isStaticMethod()) 
            interpretStaticMethodInsn(c, mi);
        else interpretNonStaticMethodInsn(c, mi);
    }

    static void interpretIincInsn(AbstractInterpretationContext c, IincInsn i) {}

    static void interpretIntInsn(AbstractInterpretationContext c, IntInsn i) {}

    static void interpretJumpInsn(AbstractInterpretationContext c, JumpInsn i) {
        switch(i.opcode) {
        case Opcodes.IFEQ: 
        case Opcodes.IFNE: 
        case Opcodes.IFLT: 
        case Opcodes.IFGE: 
        case Opcodes.IFGT: 
        case Opcodes.IFLE: {
            c.popStack(i);
            c.addBranchTarget(new AbstractInterpretationContext(i, c, c.getNext(i)));
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
            c.popStack(i);
            c.popStack(i);
            c.addBranchTarget(new AbstractInterpretationContext(i, c, c.getNext(i)));
            break;
        }
        case Opcodes.GOTO: {
            c.pc = c.getNext(i);
            break;
        }
        case Opcodes.JSR: NYI(i); break;
        case Opcodes.IFNULL: 
        case Opcodes.IFNONNULL:  {
            c.popStack(i);
            c.addBranchTarget(new AbstractInterpretationContext(i, c, c.getNext(i)));
            break;
        }
        default: NYI(i); 
        }
    }

    static void interpretLabelInsn(AbstractInterpretationContext c, LabelInsn i) {}

    static void interpretLdcInsn(AbstractInterpretationContext c, LdcInsn i) {
        Object o = i.cst;
        if (o instanceof Integer)     c.pushStackDefinition(i, "I");
        else if (o instanceof Long)   c.pushStackDefinition(i, "J");
        else if (o instanceof Float)  c.pushStackDefinition(i, "F");
        else if (o instanceof Double) c.pushStackDefinition(i, "D");
        else {
            // Is this right? dr2chase - 2011-04-08
            // It doesn't really matter for what we are doing.  CHF 
            String cl = o.getClass().getName().replace(".", "/");
            c.pushStackDefinition(i, "L"+ cl + ";");
            }
        }

    static void interpretLookupSwitchInsn(AbstractInterpretationContext c, LookupSwitchInsn i) {}
 
    static void interpretSingleInsn(AbstractInterpretationContext c, SingleInsn i) {
            switch(i.opcode) {
            case Opcodes.NOP: break;
            case Opcodes.ACONST_NULL: c.pushStackDefinition(i, "NULL"); break;
            case Opcodes.ICONST_M1: 
            case Opcodes.ICONST_0: 
            case Opcodes.ICONST_1: 
            case Opcodes.ICONST_2: 
            case Opcodes.ICONST_3: 
            case Opcodes.ICONST_4: 
            case Opcodes.ICONST_5: c.pushStackDefinition(i, "I");break;
            case Opcodes.LCONST_0: 
            case Opcodes.LCONST_1: c.pushStackDefinition(i, "J");break;
            case Opcodes.FCONST_0: 
            case Opcodes.FCONST_1: 
            case Opcodes.FCONST_2: c.pushStackDefinition(i, "F"); break;
            case Opcodes.DCONST_0: 
            case Opcodes.DCONST_1: c.pushStackDefinition(i, "D"); break;
            case Opcodes.IALOAD:   c.pushStackDefinition(i, "I"); break;
            case Opcodes.LALOAD:   c.pushStackDefinition(i, "J"); break;
            case Opcodes.FALOAD:   c.pushStackDefinition(i, "F"); break;
            case Opcodes.DALOAD:   c.pushStackDefinition(i, "D"); break;
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
            case Opcodes.POP: c.popStack(i); break; 
            case Opcodes.POP2: NYI(i); break; 
            case Opcodes.DUP: {
                AbstractInterpretationValue dup_x_value = c.popStack(i); 
                c.pushStack(i, dup_x_value); 
                c.pushStack(i, dup_x_value); 
                break;
            }
            case Opcodes.DUP_X1: NYI(i); break; 
            case Opcodes.DUP_X2: NYI(i); break; 
            case Opcodes.DUP2: NYI(i); break; 
            case Opcodes.DUP2_X1: NYI(i); break; 
            case Opcodes.DUP2_X2: NYI(i); break; 
            case Opcodes.SWAP: {
                AbstractInterpretationValue swap_x_value = c.popStack(i);
                AbstractInterpretationValue swap_y_value = c.popStack(i);
                c.pushStack(i, swap_y_value);
                c.pushStack(i, swap_x_value);
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
            case Opcodes.IRETURN: c.popStack(i); c.finished(); break;
            case Opcodes.LRETURN: c.popStack(i); c.popStack(i); c.finished(); break; 
            case Opcodes.FRETURN: c.popStack(i); c.finished(); break; 
            case Opcodes.DRETURN: c.popStack(i); c.popStack(i); c.finished(); break;
            case Opcodes.ARETURN: c.popStack(i); c.finished(); break; 
            case Opcodes.RETURN:  c.finished(); break;
            case Opcodes.ARRAYLENGTH: NYI(i); break; 
            case Opcodes.ATHROW: c.popStack(i); c.finished(); break; 
            case Opcodes.MONITORENTER: c.popStack(i); break; 
            case Opcodes.MONITOREXIT:  c.popStack(i); break;
            default: 
                NYI(i);
            }
        }


    static void interpretTableSwitchInsn(AbstractInterpretationContext c, TableSwitchInsn i) {NYI(i);}

    static void interpretTryCatchBlock(AbstractInterpretationContext c, TryCatchBlock i) { 
            // for every instruction from i.start to i.end i.handler is a potential next instruction.
            //        For now just make sure the handler is evaluated.
        Integer handler = c.getJumpDestination(i.handler.toString());
        AbstractInterpretationContext handlerContext = new AbstractInterpretationContext(i, c, handler);
        handlerContext.pushStackDefinition(i, "Ljava.lang.Throwable;");
        c.addBranchTarget(handlerContext);
    }

    static void interpretTypeInsn(AbstractInterpretationContext c, TypeInsn i) {
        switch (i.opcode) {
        case Opcodes.NEW: c.pushStackDefinition(i, i.type); break;
        case Opcodes.ANEWARRAY: NYI(i); break;
        case Opcodes.CHECKCAST: {
            AbstractInterpretationValue v = c.popStack(i); 
            if (v != null) {
                if (v.getType().equals(i.getType()))
                    c.pushStack(i, v);
                else {
                    c.pushStackDefinition(i, i.type); break;
                }
            } else {
                throw new RuntimeException("Checkcast called on null value");
            }
            break;
        }
        case Opcodes.INSTANCEOF: c.popStack(i); c.pushStackDefinition(i, "I"); break;
        default: NYI(i); break;
        }
    }

    static void interpretVisitLineNumberInsn(AbstractInterpretationContext c, VisitLineNumberInsn i) {}

    static void NYI(Insn i) {
        throw new RuntimeException("Abstract Interpretation: Insn " + i + " not yet implemented");
    }

}