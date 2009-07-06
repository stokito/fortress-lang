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

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeFactory;
// import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.Debug;

import static com.sun.fortress.exceptions.ProgramError.errorMsg;

/**
 * When we call a task, we first set up all the local varibles we are interested in as
   fields in the task.  Then when we want to access them we do so via fieldrefs.  Eventually
   we will need to write the values back out.  
*/

public class TaskVarCodeGen extends VarCodeGen {
    final String taskClass;
    private final APIName ifNone;

    public TaskVarCodeGen(VarCodeGen v, String taskClass, APIName ifNone) {
        super(v.name, v.fortressType);
        this.taskClass = taskClass;
        this.ifNone = ifNone;
        System.out.println("Creating a new TaskVarCodeGen from VarCodeGen " + v);
    }

    public TaskVarCodeGen(IdOrOp name, Type fortressType, String taskClass, APIName ifNone) {
        super(name, fortressType);
        this.ifNone = ifNone;
        this.taskClass = taskClass;
    }

    public void pushValue(CodeGenMethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, mv.getLocalVariable("instance"));
        mv.visitFieldInsn(Opcodes.GETFIELD, taskClass, 
                          name.getText(), 
                          NamingCzar.only.jvmTypeDesc(fortressType, ifNone)) ;

    }

    public void assignValue(CodeGenMethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ASTORE, mv.getLocalVariable(name.getText()));
        mv.visitVarInsn(Opcodes.ALOAD, mv.getLocalVariable("instance"));
        mv.visitVarInsn(Opcodes.ALOAD, mv.getLocalVariable(name.getText()));
        mv.visitFieldInsn(Opcodes.PUTFIELD, taskClass, 
                          name.getText(), 
                          NamingCzar.only.jvmTypeDesc(fortressType, ifNone) 
                          );
    }

    public void outOfScope(CodeGenMethodVisitor mv) {
        // I dunno what this is for.
        Label finish = new Label();
    }

}