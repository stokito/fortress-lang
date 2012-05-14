/*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;
import com.sun.fortress.compiler.NamingCzar;

public class MethodInsn extends Insn {
    int opcode;
    String owner;
    String _name;
    String desc;
    int id;

    static int counter = 0;

    MethodInsn(String name, int opcode, String owner, String _name, String desc, String index) {
        super(name,index);
        this.opcode = opcode;
        this.owner = owner;
        this._name = _name;
        this.desc = desc;
        this.id = counter++;
    }

    public MethodInsn copy(String newIndex) {
        return new MethodInsn(name, opcode, owner, _name, desc, newIndex);
    }

    public String toString() { 
        return "MethodInsn: " + id + " " + index + " " + opcode + " " + owner + " " + _name + " " + desc;
    }

//         if (def != null && def.needed) {
//             return "MethodInsn:" + id + " " + index + " " +  opcode + " " + owner + " " + _name + " " + desc + " is used in a definition and is needed";
//         } else if (def != null) {
//             return "MethodInsn:" + id + " " + index + " " +  opcode + " " + owner + " " + _name + " " + desc + " is used in a definition, but the definition isn't needed";            
//         } else return "MethodInsn:" + id + " " + index + " " +  opcode + " " + owner + " " + _name + " " + desc + " is not used in a definition";
//     }
    
    public void toAsm(MethodVisitor mv) { 
        mv.visitMethodInsn(opcode, owner, _name, desc);
    }

    public boolean matches(int opcode, String owner, String _name, String desc) {
        return ((this.opcode == opcode) &&
                (this.owner.equals(owner)) &&
                (this._name.equals(_name)) &&
                (this.desc.equals(desc)));
    }

    public boolean matches(MethodInsn mi) {
        return matches(mi.opcode, mi.owner, mi._name, mi.desc);
    }

    public boolean isBoxingMethod() {
        return _name.equals("make") &
            NamingCzar.typeIsFortressSpecialType(owner);
    }

    public boolean isFVoidBoxingMethod() {
        return this.owner.equals("com/sun/fortress/compiler/runtimeValues/FVoid") &&
            _name.equals("make");
    }

    public boolean isUnBoxingMethod() {
        if (_name.equals("getValue") & NamingCzar.typeIsFortressSpecialType(owner))
            return true;
        return false;
    }

    private boolean needed() {
        boolean result = false;
        for (AbstractInterpretationValue def : getDefs())
            if (def.isNeeded()) 
                result = true;
        return result;
    }

    public boolean isUnNeededBoxingMethod() {
        if (isBoxingMethod()) {
            if (!needed())
                return true;
        }
        return false;
    }

    public boolean isUnNeededUnBoxingMethod() {
        if (isUnBoxingMethod()) {
            if (!needed())
                return true;
        }
        return false;
    }


    public boolean isStaticMethod() {
        return this.opcode == Opcodes.INVOKESTATIC;
    }

    public boolean isInterfaceMethod() {
        return this.opcode == Opcodes.INVOKEINTERFACE;
    }

    public void interpret(ByteCodeMethodVisitor bcmv) {
    }
}
