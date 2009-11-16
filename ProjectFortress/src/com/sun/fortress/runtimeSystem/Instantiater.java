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
    
    InstantiationMap xlation;
    String instanceName;
    
    public Instantiater(ClassVisitor cv, Map xlation, String instanceName) {
        super(cv);
        this.xlation = new InstantiationMap(xlation);
        this.instanceName = instanceName;
    }

    
    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        // TODO Auto-generated method stub
        String[] new_interfaces = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            new_interfaces[i] = xlation.getCompletely(interfaces[i]);
        }
        super.visit(version, access, instanceName, signature,
                xlation.getCompletely(superName), new_interfaces);
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
        desc = xlation.getDesc(desc);
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
        name = xlation.getCompletely(name);
        desc = xlation.getDesc(desc);
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

        return new MethodInstantiater(mv, xlation);
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
