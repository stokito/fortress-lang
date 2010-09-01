/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
    public void visitFieldInsn(int opcode, String owner, String name,
            String desc) {

        owner = Naming.mangleFortressIdentifier(owner);
        name = Naming.mangleMemberName(name);
        desc = Naming.mangleFortressDescriptor(desc);

        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature,
            Label start, Label end, int index) {

        signature = Naming.mangleFortressIdentifier(signature);
        name = Naming.mangleMemberName(name);
        desc = Naming.mangleFortressDescriptor(desc);

        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
            String desc) {
        owner = Naming.mangleFortressIdentifier(owner);
        name = Naming.mangleMemberName(name);
        desc = Naming.mangleMethodSignature(desc);

        super.visitMethodInsn(opcode, owner, name, desc);
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
        super.visitEnd();
    }

}
