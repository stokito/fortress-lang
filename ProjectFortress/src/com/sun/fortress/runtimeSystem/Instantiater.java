/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.runtimeSystem;

import java.util.Map;
import java.util.regex.Pattern;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import com.sun.fortress.useful.StringMap;
import com.sun.fortress.useful.Useful;

public class Instantiater extends ClassAdapter {
    
    InstantiationMap types;
    String instanceName;
    InstantiatingClassloader icl;
    
    public Instantiater(ClassVisitor cv, Map xlation, String instanceName, InstantiatingClassloader icl) {
        super(cv);
        this.types = new InstantiationMap(xlation);
        this.instanceName = instanceName;
        this.icl = icl;
    }

    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        // TODO Auto-generated method stub
        String[] new_interfaces = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            new_interfaces[i] = types.getTypeName(interfaces[i]);
        }
        super.visit(version, access,
        types.getTypeName(name),
        // instanceName, 
        signature,
                types.getTypeName(superName), new_interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        // TODO Auto-generated method stub
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        // TODO Auto-generated method stub
        super.visitAttribute(attr);
    }

    @Override
    public void visitEnd() {
        // TODO Auto-generated method stub
        super.visitEnd();
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc,
            String signature, Object value) {
        // TODO Auto-generated method stub
        desc = types.getFieldDesc(desc);
        name = types.getName(name);  // ? do we rewrite the name of a field?
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public void visitInnerClass(String name, String outerName,
            String innerName, int access) {
        // TODO Auto-generated method stub
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        // necessary?
        name = types.getName(name);
        desc = types.getMethodDesc(desc);
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

        return new MethodInstantiater(mv, types, icl);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        // TODO Auto-generated method stub
        super.visitOuterClass(owner, name, desc);
    }

    @Override
    public void visitSource(String source, String debug) {
        // TODO Auto-generated method stub
        super.visitSource(source, debug);
    }


}
