/*
 * Created on Aug 1, 2011
 *
 */
package com.sun.fortress.runtimeSystem;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

abstract public class InitializedStaticField implements Opcodes {
    static final class StaticForClosureField extends InitializedStaticField {
        private final String field_desc;
    
        private final String final_name;
    
        StaticForClosureField(String field_desc, String final_name) {
            this.field_desc = field_desc;
            this.final_name = final_name;
        }
    
        @Override
        public void forClinit(MethodVisitor init_mv) {
            init_mv.visitTypeInsn(NEW, final_name);
            init_mv.visitInsn(DUP);
            init_mv.visitMethodInsn(INVOKESPECIAL, final_name, "<init>", "()V");
            init_mv.visitFieldInsn(PUTSTATIC, final_name, Naming.CLOSURE_FIELD_NAME, field_desc);
        }
    
        @Override
        public String asmName() {
            return Naming.CLOSURE_FIELD_NAME;
        }
    
        @Override
        public String asmSignature() {
            return field_desc;
        }
    }

    static final class StaticForRttiFieldOfTuple extends
            InitializedStaticField {
        private final String classname;
        private final InstantiatingClassloader icl;
    
        StaticForRttiFieldOfTuple(String classname, InstantiatingClassloader icl) {
            this.classname = classname;
            this.icl = icl;
        }
    
        @Override
        public void forClinit(MethodVisitor mv) {
        	MethodInstantiater mi = new MethodInstantiater(mv, null, icl);
        	mi.rttiReference(classname + Naming.RTTI_CLASS_SUFFIX);	
            mv.visitFieldInsn(PUTSTATIC, classname, Naming.RTTI_FIELD, Naming.RTTI_CONTAINER_DESC);
        }
    
        @Override
        public String asmName() {
            return Naming.RTTI_FIELD;
        }
    
        @Override
        public String asmSignature() {
            return Naming.RTTI_CONTAINER_DESC;
        }
    }

    public static final class StaticForJLOParameterizedRttiField extends
            InitializedStaticField {
        private final String final_name;
    
        StaticForJLOParameterizedRttiField(String final_name) {
            this.final_name = final_name;
        }
    
        @Override
        public void forClinit(MethodVisitor init_mv) {
        	init_mv.visitTypeInsn(NEW, Naming.JAVA_RTTI_CONTAINER_TYPE);
        	init_mv.visitInsn(DUP);
        	init_mv.visitLdcInsn(Type.getType(Naming.mangleFortressDescriptor("Ljava/lang/Object;")));
        	init_mv.visitMethodInsn(INVOKESPECIAL, Naming.JAVA_RTTI_CONTAINER_TYPE, "<init>", "(Ljava/lang/Class;)V");
        	init_mv.visitFieldInsn(PUTSTATIC, final_name, Naming.RTTI_FIELD, Naming.RTTI_CONTAINER_DESC);
        }
    
        @Override
        public String asmName() {
            return Naming.RTTI_FIELD;
        }
    
        @Override
        public String asmSignature() {
            return Naming.RTTI_CONTAINER_DESC;
        }
    }

    static final class StaticForUsualRttiField extends
            InitializedStaticField {
        private final String final_name;
        private final InstantiatingClassloader icl;
        
        StaticForUsualRttiField(String final_name, InstantiatingClassloader icl) {
            this.final_name = final_name;
            this.icl = icl;
        }
    
        @Override
        public void forClinit(MethodVisitor init_mv) {
        	MethodInstantiater mi = new MethodInstantiater(init_mv, null, icl);
        	mi.rttiReference(final_name + Naming.RTTI_CLASS_SUFFIX);
        	init_mv.visitFieldInsn(PUTSTATIC, final_name, Naming.RTTI_FIELD, Naming.RTTI_CONTAINER_DESC);
        }
    
        @Override
        public String asmName() {
            return Naming.RTTI_FIELD;
        }
    
        @Override
        public String asmSignature() {
            return Naming.RTTI_CONTAINER_DESC;
        }
    }

    abstract public void forClinit(MethodVisitor mv);

    abstract public String asmName();

    abstract public String asmSignature();
}