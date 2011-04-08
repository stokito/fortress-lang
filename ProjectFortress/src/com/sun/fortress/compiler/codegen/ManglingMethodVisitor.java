/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.codegen;

import com.sun.fortress.compiler.nativeInterface.SignatureParser;

// This class allows us to wrap MethodVisitor.visitMaxs Methods to
// dump bytecodes.  It is generally used with CodeGenClassWriter.

import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.useful.Debug;

public class ManglingMethodVisitor extends MethodAdapter {

    String name, desc;
    int access;
    
    public ManglingMethodVisitor(MethodVisitor mvisitor, int access, String name, String desc) {
        super(mvisitor);
        this.access = access;
        this.name = name;
        this.desc = desc;
    }

    public void visitMaxs(int maxStack, int maxLocals) {
        /* 
         * Print early, before it goes bad.
         */
        if (mv instanceof TraceMethodVisitor) {
            System.out.println(name + desc + " " + Integer.toHexString(access));
            List t = ((TraceMethodVisitor)mv).getText();
            for (Object s : t)
                System.out.print(s);
        }
        super.visitMaxs(maxStack, maxLocals);
    }


    @Override
    public void visitFieldInsn(int opcode, String owner, String _name,
            String desc) {

        owner = Naming.mangleFortressIdentifier(owner);
        _name = Naming.mangleMemberName(_name);
        desc = Naming.mangleFortressDescriptor(desc);

        super.visitFieldInsn(opcode, owner, _name, desc);
    }

    @Override
    public void visitLocalVariable(String _name, String desc, String signature,
            Label start, Label end, int index) {

        signature = Naming.mangleFortressIdentifier(signature);
        _name = Naming.mangleMemberName(_name);
        desc = Naming.mangleFortressDescriptor(desc);

        super.visitLocalVariable(_name, desc, signature, start, end, index);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String _name,
            String desc) {
        owner = Naming.mangleFortressIdentifier(owner);
        _name = Naming.mangleMemberName(_name);
        desc = Naming.mangleMethodSignature(desc);

        super.visitMethodInsn(opcode, owner, _name, desc);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        desc = Naming.mangleFortressIdentifier(desc);
        super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler,
            String type) {
        // TODO Auto-generated method stub
        // need to do more here, eventually
        super.visitTryCatchBlock(start, end, handler, type);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        type = Naming.mangleFortressIdentifier(type);

        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitEnd() {
        
        // Special case here for abstract methods
        if (mv instanceof TraceMethodVisitor && 0 != (access & Opcodes.ACC_ABSTRACT) ) {
            System.out.println(name + desc + " " + Integer.toHexString(access));
            List t = ((TraceMethodVisitor)mv).getText();
            for (Object s : t)
                System.out.print(s);
        }
        
        super.visitEnd();
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if (cst instanceof Type) {
            Type type = (Type) cst;
            String desc = type.getDescriptor();
            // this fails because Asm Type.getType fails for generic types
            // desc = Naming.mangleFortressDescriptor(desc);
            cst = Type.getType(desc);
        }
        super.visitLdcInsn(cst);
    }

}
