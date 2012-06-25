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
import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.exceptions.CompilerError;

public class CodeGenMethodVisitor extends TraceMethodVisitor {
    // All these fields are passed to the constructor, but never
    // used except possibly for debugging after the constructor has run.
    private int access;
    private String name;
    private String desc;
    private String signature;
    private String[] exceptions;
    private List<String> argumentTypes;
    private String resultType;

    // Only these fields are actually required to generate code.
    int localVariableCount;
    boolean hasThis;

    // This stuff is kept around to emit debugging information.
    // All these lists are managed stack-fashion and indexed by handle.
    private List<String> varNames;
    private List<String> varTypes;
    private List<Label> varFirstUse;

    public CodeGenMethodVisitor(int access, String name, String desc,
                                String signature, String[] exceptions,
                                MethodVisitor mvisitor) {
        super(mvisitor);
        this.access = access;
        this.name = name;
        this.desc = desc;
        this.signature = signature;
        this.exceptions = exceptions;
        this.argumentTypes = NamingCzar.parseArgs(desc);
        this.resultType = NamingCzar.parseResult(desc);
        this.localVariableCount = 0;

        int sz = 5 + this.localVariableCount + this.argumentTypes.size();
        this.varNames = new ArrayList(sz);
        this.varTypes = new ArrayList(sz);
        this.varFirstUse = new ArrayList(sz);

        this.hasThis = (access & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC;

        Debug.debug(Debug.Type.CODEGEN, 2,
                    "MethodVisitor: name = " , name , " desc = " , desc ,
                    " argumentTypes = " , argumentTypes , " resultType " , resultType);

    }

    public void dump() {
        Debug.debug(Debug.Type.CODEGEN, 2,
                    "MethodVisitor: name = " , name , " desc = " , desc ,
                    " access = " , access , " signature = " , signature ,
                    " exceptions " , exceptions ,
                    " argumentTypes = " , argumentTypes , " resultType " , resultType);
    }

    public void visitMaxs(int maxStack, int maxLocals) {
        dumpBytecodes();
        super.visitMaxs(maxStack, maxLocals);
    }

    public void dumpBytecodes() {
        Debug.debug(Debug.Type.CODEGEN, 2, getText());
    }

    private String getStackTrace() {
        String result = "\n";
        StackTraceElement[] ste = Thread.currentThread().getStackTrace();

        for (StackTraceElement s : ste) result = result + s + "\n";
        return result;
    }

    private String opc(int opcode) {
        return com.sun.fortress.compiler.asmbytecodeoptimizer.Opcodes.opcNames[opcode];
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) 
    {
        Debug.debug(Debug.Type.CODEGEN, 2, "visitFieldInsn: ", opc(opcode), " owner = " + owner, " name = ", name, " desc = ", desc, getStackTrace());
        super.visitFieldInsn(opcode, owner,name,desc);
    }

    public void visitIincInsn(int var, int increment) {
        Debug.debug(Debug.Type.CODEGEN, 2, "visitIincInsn: var = ", var , " increment = ", increment, getStackTrace());
        super.visitIincInsn(var, increment);
    }

    public void visitInsn(int opcode) {
        Debug.debug(Debug.Type.CODEGEN, 2, "visitInsn:", opc(opcode), getStackTrace());
        super.visitInsn(opcode);
    }
    
    public void visitIntInsn(int opcode, int operand) {
        Debug.debug(Debug.Type.CODEGEN, 2, "visitIntInsn:", opc(opcode), " ", operand, " " , getStackTrace());
        super.visitIntInsn(opcode, operand);
    }

    public void visitJumpInsn(int opcode, Label label) {
        Debug.debug(Debug.Type.CODEGEN, 2, "visitJumpInsn:", opc(opcode), " ", label, " " , getStackTrace());
        super.visitJumpInsn(opcode, label);
    }

    public void visitLabel(Label label) {
        Debug.debug(Debug.Type.CODEGEN, 2, "visitLabel:", label);
        super.visitLabel(label);
    }

    public void visitLdcInsn(Object cst) {
        Debug.debug(Debug.Type.CODEGEN, 2, "visitLdcInsn:", cst);
        super.visitLdcInsn(cst);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        Debug.debug(Debug.Type.CODEGEN, 2, "visitMethodInsn:", opc(opcode), " owner = ", owner, " name = ", name, " desc = ", desc, getStackTrace());
        super.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitTypeInsn(int opcode, String type) {
        Debug.debug(Debug.Type.CODEGEN, 2, "visitTypeInsn:", opc(opcode), " type = ",  type, getStackTrace());
        super.visitTypeInsn(opcode, type);
    }

    public void visitVarInsn(int opcode, int var) {
        Debug.debug(Debug.Type.CODEGEN, 2, "visitVarInsn:", opc(opcode), " var = " , var, getStackTrace());
        super.visitVarInsn(opcode, var);
    }

    /**
     * the correct way to get a local is to call createCompilerLocal,
     * which will give you a stack offset that acts as a handle on the
     * local variable you requested, and then to hand that local
     * variable back again when you're done by calling
     * disposeCompilerLocal so that debug information can be generated
     * where possible, and so that stack slots can be reused in a
     * sensible way.  If you are representing a variable in Fortress
     * source code, you should do this in the constructor for a
     * VarCodeGen; CodeGen maintains a lexEnv that maps a Fortress
     * variable to the corresponding VarCodeGen. */

    // Non-user-accessible local variable
    public int createCompilerLocal(String _name, String type) {
        if (localVariableCount != varNames.size()) {
            throw new CompilerError("Trying to create local " + _name +
                                       " but current metadata is off\nlocalVariableCount = " +
                                       localVariableCount +
                                       "\nvarNames = " + varNames);
        }
        Debug.debug(Debug.Type.CODEGEN, 2,
                    "LOCAL create ", localVariableCount, " ", _name);
        varNames.add(_name);
        varTypes.add(type);
        Label start = new Label();
        visitLabel(start);
        varFirstUse.add(start);

        return localVariableCount++;
    }

    // I need to reserve slot 0.  
    public int reserveSlot0() {
        return createCompilerLocal("this", null);
    }

    public void disposeCompilerLocal(int localId) {
        if (localId >= localVariableCount) {
            throw new CompilerError("Trying to dispose of local " + localId +
                                       " but current metadata is off\nlocalVariableCount = " +
                                       localVariableCount +
                                       "\nvarNames = " + varNames);
        }
        String ty = varTypes.get(localId);
        if (ty != null) {
            Label finish = new Label();
            visitLabel(finish);
            visitLocalVariable(varNames.get(localId), ty, null, varFirstUse.get(localId), finish, localId);
        }
        Debug.debug(Debug.Type.CODEGEN, 2,
                    "LOCAL destroy ", localId, " ", varNames.get(localId));
        if (localId == (localVariableCount-1)) {
            varNames.remove(localId);
            varTypes.remove(localId);
            varFirstUse.remove(localId);
            localVariableCount--;
        } else {
            varNames.set(localId, null);
            varTypes.set(localId, null);
            varFirstUse.set(localId, null);
        }
    }

    public int getThis() {
        if (hasThis) {
            return 0;
        } else {
            throw new CompilerError("Trying to get this/self in static method.");
        }
    }

}
