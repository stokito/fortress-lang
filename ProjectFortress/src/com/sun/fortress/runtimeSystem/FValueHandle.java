/********************************************************************************
 Copyright 2012, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ********************************************************************************/

package com.sun.fortress.runtimeSystem;

import com.sun.fortress.compiler.codegen.CodeGenMethodVisitor;
import com.sun.fortress.compiler.runtimeValues.FValue;

import org.objectweb.asm.Opcodes;

public abstract class FValueHandle {
    CodeGenMethodVisitor mv;
    public void checkValue(FValue val) {
        mv.visitLdcInsn(val);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                           "com.sun.fortress.runtimeSystem.TransactionRecord",
                           "checkValue",
                           "(Lcom.sun.fortress.compiler.runtimeValues.FValue;Lcom.sun.fortress.compiler.runtimeValues.FValue;)()");
    }

    public FValueHandle(CodeGenMethodVisitor mv) {
        this.mv = mv;
    }

    public class LocalFValueHandle extends FValueHandle{
        int offset;

        public void checkValue(FValue val) {
            mv.visitVarInsn(Opcodes.ALOAD, offset);
            super.checkValue(val);
        }

        LocalFValueHandle(CodeGenMethodVisitor mv, int offset) {
            super(mv);
            this.offset = offset;
        }
    }

    public class FieldFValueHandle extends FValueHandle {
        Object obj;
        String packageAndClassName;
        String objectFieldName;
        String classDesc;

        public void checkValue(FValue val) {
            mv.visitLdcInsn(obj);
            mv.visitFieldInsn(Opcodes.GETFIELD, packageAndClassName, 
                              objectFieldName, classDesc);
            super.checkValue(val);
        }
        
        FieldFValueHandle(CodeGenMethodVisitor mv, Object obj, 
                          String packageAndClassName, String objectFieldName, 
                          String classDesc) {
            super(mv);
            this.obj = obj;
            this.packageAndClassName = packageAndClassName;
            this.objectFieldName = objectFieldName;
            this.classDesc = classDesc;
        }
    }
}
        
                               
        