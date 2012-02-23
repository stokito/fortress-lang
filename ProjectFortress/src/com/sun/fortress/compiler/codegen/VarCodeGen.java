/*******************************************************************************
    Copyright 2009,2012, Oracle and/or its affiliates.
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
    protected final String name;
    public final Type fortressType;
    public final int sizeOnStack;

    public VarCodeGen(String name, Type fortressType) {
        super();
        this.name = name;
        this.fortressType = fortressType;
        // TODO: compute sizeOnStack correctly for non-pointer types.
        this.sizeOnStack = 1;
    }

    public static String varCGName(IdOrOp nm, List<StaticArg> lsargs) {
        return NamingCzar.idOrOpToString(nm) +
            (lsargs != null && lsargs.size() > 0 ? lsargs.toString() : "");
    }
    
    public VarCodeGen(IdOrOp name, List<StaticArg> lsargs, Type fortressType) {
        this(varCGName(name, lsargs), fortressType);
    }

    public VarCodeGen(IdOrOp name, Type fortressType) {
        this(name.getText(), fortressType);
    }

    public String getName() {
        return name;
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

        public void assignValue(CodeGenMethodVisitor mv) {
            mv.visitVarInsn(Opcodes.ASTORE, offset);
        }

        public void outOfScope(CodeGenMethodVisitor mv) {
            mv.disposeCompilerLocal(offset);
        }

        public String toString() {
            return "VarCodeGen:StackVar" + name + "," + fortressType;
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
        
        public NeedsType(IdOrOp id, List<StaticArg> lsargs, Type fortressType, String owner, String name, String desc) {
            super(id, lsargs, fortressType);
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

        public void assignValue(CodeGenMethodVisitor mv) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.SWAP);
            mv.visitFieldInsn(Opcodes.PUTFIELD, packageAndClassName, objectFieldName, classDesc);
        }

        public String toString() {
            return "VarCodeGen:FieldVar" + name + "," + fortressType;
        }

        @Override
        public void outOfScope(CodeGenMethodVisitor mv) {
            // TODO Auto-generated method stub

        }

    }

    public static class MutableFieldVar extends NeedsType {
        public MutableFieldVar(IdOrOp id, Type fortressType, String owner, String name, String desc) {
            super(id, fortressType, owner, name, desc);
        }

        public void pushValue(CodeGenMethodVisitor mv) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, packageAndClassName, objectFieldName, classDesc);
        }


        public void assignValue(CodeGenMethodVisitor mv) {
            // URG, another case of meeting assumptions.
            // We get here with the value to assign on the stack, but not the 
            // object, so load the object, and then swap them, and add a return val
            // to keep the rest of codegen happy.
            mv.visitVarInsn(Opcodes.ALOAD,0);
            mv.visitInsn(Opcodes.SWAP);
            mv.visitFieldInsn(Opcodes.PUTFIELD, packageAndClassName, objectFieldName, classDesc);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, NamingCzar.internalFortressVoid, NamingCzar.make,
                               Naming.makeMethodDesc("", NamingCzar.descFortressVoid));
        }

        public String toString() {
            return "VarCodeGen:MutableFieldVar" + name + "," + fortressType;
        }


        @Override
        public void outOfScope(CodeGenMethodVisitor mv) {
            // TODO Auto-generated method stub

        }

    }

    public static class StaticBinding extends NeedsType {

        List<String> sparams;

        private StaticBinding(IdOrOp id, Type fortressType, String owner, String name, String desc, List<String> sparams) {
            super(id, fortressType, owner, name, desc);
            this.sparams = sparams;
        }

        public StaticBinding(IdOrOp id, Type fortressType, String owner, String name, String desc) {
            super(id, fortressType, owner, name, desc);
            this.sparams = Collections.emptyList();
        }

        public StaticBinding(IdOrOp id, List<StaticArg> lsargs, Type fortressType, String owner, String name, String desc) {
            super(id, lsargs, fortressType, owner, name, desc);
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

        public void assignValue(CodeGenMethodVisitor mv) {
            throw new CompilerError(errorMsg("Invalid assignment to static binding ",name,
                    ": ", fortressType));
        }

        public String toString() {
            return "VarCodeGen:StaticBinding" + name + "," + fortressType;
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

        public void assignValue(CodeGenMethodVisitor mv) {
            throw new CompilerError(errorMsg("Invalid assignment to ",name,
                                             ": ", fortressType,
                                             " param ",offset,
                                             " size ", sizeOnStack));
        }

        public String toString() {
            return "VarCodeGen:ParamVar" + name + "," + fortressType;
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

        public String toString() {
            return "VarCodeGen: localVar: " + name + "," + fortressType;
        }

    }

    public static class LocalMutableVar extends StackVar {

        public LocalMutableVar(IdOrOp id, Type fortressType, CodeGen cg) {
            super(id, fortressType, cg);
        }
    
        public void assignValue(CodeGenMethodVisitor mv) {
            // Ugh!  We are compensating for an extra value on the stack in doStatements in CodeGen
            // So we need to ensure that there is one.
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ASTORE, offset);
        }

        public void pushValue(CodeGenMethodVisitor mv) {
            super.pushValue(mv);
        }

        public String toString() {
            return "VarCodeGen: localMutableVar: " + name + "," + fortressType;
        }

    }

    public static class TaskVarCodeGen extends VarCodeGen {
        final String taskClass;
        private final APIName ifNone;

        public TaskVarCodeGen(VarCodeGen v, String taskClass, APIName ifNone) {
            super(v.name, v.fortressType);
            this.taskClass = taskClass;
            this.ifNone = ifNone;
            Debug.debug(Debug.Type.CODEGEN, 1,
                        "Creating a new TaskVarCodeGen from VarCodeGen " + v);
        }

        public TaskVarCodeGen(IdOrOp name, Type fortressType, String taskClass, APIName ifNone) {
            super(name, fortressType);
            this.ifNone = ifNone;
            this.taskClass = taskClass;
        }

        public void pushValue(CodeGenMethodVisitor mv) {
            mv.visitVarInsn(Opcodes.ALOAD, mv.getThis());
            mv.visitFieldInsn(Opcodes.GETFIELD, taskClass,
                              getName(),
                              NamingCzar.jvmTypeDesc(fortressType, ifNone)) ;

        }

        public void assignValue(CodeGenMethodVisitor mv) {
            mv.visitVarInsn(Opcodes.ALOAD, mv.getThis());
            mv.visitInsn(Opcodes.SWAP);
            mv.visitFieldInsn(Opcodes.PUTFIELD, taskClass,
                              getName(),
                              NamingCzar.jvmTypeDesc(fortressType, ifNone)
                              );
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, NamingCzar.internalFortressVoid, NamingCzar.make,
                           Naming.makeMethodDesc("", NamingCzar.descFortressVoid));
        }

        public void outOfScope(CodeGenMethodVisitor mv) {
            // We've already told asm about our type and such.
        }

    }
}