/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.MethodVisitor;

import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeFactory;
// import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.runtimeSystem.InstantiationMap;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.compiler.NamingCzar;

import static com.sun.fortress.exceptions.ProgramError.errorMsg;

/**
 * VarCodeGen stores necessary information about a (local) variable
 * and generates code for references to that variable.
 */
public abstract class VarCodeGen {
    public final IdOrOp name;
    public final Type fortressType;
    public final int sizeOnStack;

    public VarCodeGen(IdOrOp name, Type fortressType) {
        super();
        this.name = name;
        this.fortressType = fortressType;
        // TODO: compute sizeOnStack correctly for non-pointer types.
        this.sizeOnStack = 1;
    }

    public String toString() {
        return "VarCodeGen:" + name + "," + fortressType;
    }

    /** Generate code to push the value of this variable onto the Java stack.
     */
    public abstract void pushValue(CodeGenMethodVisitor mv);

    public void pushValue(CodeGenMethodVisitor mv, String static_args) {
        if (static_args.length() == 0)
            pushValue(mv);
        else
            throw new CompilerError(errorMsg("Unexpected static args supplied to " + name +", statics = " + static_args));
    }

    /** Generate code to prepare to assign the value of this variable;
     *  this might push stuff on the stack.  The value can then be
     *  computed to top of stack and assignValue will perform the
     *  assignment. */
    public abstract void prepareAssignValue(CodeGenMethodVisitor mv);

    /** Generate code to assign the value of this variable from the
     *  top of the Java stack. */
    public abstract void assignValue(CodeGenMethodVisitor mv);

    /** Generate metadata after last reference to this variable. */
    public abstract void outOfScope(CodeGenMethodVisitor mv);

    /************************************************************
     * Specific kinds of Variables.
     ************************************************************/

    public static abstract class StackVar extends VarCodeGen {
        protected final int offset;

        protected StackVar(IdOrOp name, Type fortressType, CodeGen cg) {
            super(name, fortressType);
            String tyDesc = null;
            if (fortressType != null) {
                tyDesc = NamingCzar.jvmTypeDesc(fortressType, cg.thisApi(), true, true);
            }
            this.offset = cg.mv.createCompilerLocal(name.getText(), tyDesc);
        }

        public void pushValue(CodeGenMethodVisitor mv) {
            mv.visitVarInsn(Opcodes.ALOAD, offset);
        }

        public void prepareAssignValue(CodeGenMethodVisitor mv) {
            // Do nothing.
        }

        public void assignValue(CodeGenMethodVisitor mv) {
            mv.visitVarInsn(Opcodes.ASTORE, offset);
        }

        public void outOfScope(CodeGenMethodVisitor mv) {
            mv.disposeCompilerLocal(offset);
        }
    }

    protected abstract static class NeedsType extends VarCodeGen {

        protected final String packageAndClassName;
        protected final String objectFieldName;
        protected final String classDesc;

        public NeedsType(IdOrOp id, Type fortressType, String owner, String name, String desc) {
            super(id, fortressType);
            this.packageAndClassName = owner;
            this.objectFieldName = name;
            this.classDesc = desc;
        }
    }


    public static class FieldVar extends NeedsType {
        public FieldVar(IdOrOp id, Type fortressType, String owner, String name, String desc) {
            super(id, fortressType, owner, name, desc);
        }

        public void pushValue(CodeGenMethodVisitor mv) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, packageAndClassName, objectFieldName, classDesc);
        }

        public void prepareAssignValue(CodeGenMethodVisitor mv) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
        }

        public void assignValue(CodeGenMethodVisitor mv) {
            mv.visitFieldInsn(Opcodes.PUTFIELD, packageAndClassName, objectFieldName, classDesc);
        }

        @Override
        public void outOfScope(CodeGenMethodVisitor mv) {
            // TODO Auto-generated method stub

        }

    }

    public static class StaticBinding extends NeedsType {

        List<String> sparams;

        public StaticBinding(IdOrOp id, Type fortressType, String owner, String name, String desc, List<String> sparams) {
            super(id, fortressType, owner, name, desc);
            this.sparams = sparams;
        }

        public StaticBinding(IdOrOp id, Type fortressType, String owner, String name, String desc) {
            super(id, fortressType, owner, name, desc);
            this.sparams = Collections.emptyList();
        }

        public void pushValue(CodeGenMethodVisitor mv) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, packageAndClassName, objectFieldName, classDesc);
        }

        public void pushValue(CodeGenMethodVisitor mv, String static_args) {
            String cd = classDesc;

            if (static_args.length() > 0) {
                // need to rewrite the classDesc given static args.
                ArrayList<String> sargs = new ArrayList<String>();
                InstantiationMap.canonicalizeStaticParameters(static_args,
                                                              static_args.indexOf(Naming.LEFT_OXFORD),
                                                              static_args.lastIndexOf(Naming.RIGHT_OXFORD),
                                                              sargs);

                Map<String, String> xlation  = Useful.map(sparams, sargs);

                InstantiationMap im = new InstantiationMap(xlation);

                cd = im.getUnmangledTypeDesc(cd);

            }

            // TODO work in progress.
            mv.visitFieldInsn(Opcodes.GETSTATIC, packageAndClassName+static_args, objectFieldName, cd);
        }

        public void prepareAssignValue(CodeGenMethodVisitor mv) {
            throw new CompilerError(errorMsg("Invalid assignment to static binding ",name,
                    ": ", fortressType));
        }

        public void assignValue(CodeGenMethodVisitor mv) {
            prepareAssignValue(mv);
        }

        @Override
        public void outOfScope(CodeGenMethodVisitor mv) {
            // never happens
        }

    }

    /** Function parameter.  Since function parameters are immutable
     * in Fortress, we assume that we won't need other special
     * provisions for them (we'll simply copy references where
     * necessary).
     */
    public static class ParamVar extends StackVar {

        public ParamVar(IdOrOp name, Type fortressType, CodeGen cg) {
            super(name, fortressType, cg);
        }

        public void prepareAssignValue(CodeGenMethodVisitor mv) {
            throw new CompilerError(errorMsg("Invalid assignment to ",name,
                                             ": ", fortressType,
                                             " param ",offset,
                                             " size ", sizeOnStack));
        }

        public void assignValue(CodeGenMethodVisitor mv) {
            prepareAssignValue(mv);
        }
    }

    /** Local variable not visible outside current activation.  We
     *  don't presently distinguish mutable and immutable locals;
     *  perhaps we should.  A LocalVar doesn't include any variable
     *  that might scope over a local lambda, local object
     *  declaration, across work items, etc.  Those will require
     *  separate subclasses, which is one of the reasons VarCodeGen is
     *  structured this way in the first place.
     */
    public static class LocalVar extends StackVar {
        public LocalVar(IdOrOp name, Type fortressType, CodeGen cg) {
            super(name, fortressType, cg);
        }
    }
}
