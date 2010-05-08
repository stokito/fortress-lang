/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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

// This class allows us to wrap ClassWriters.
// It gives us the ability to turn bytecode debugging on and off.

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

import com.sun.fortress.runtimeSystem.Naming;

public class ManglingClassWriter extends ClassWriter {

    public ManglingClassWriter(int flags) {
        super(flags);
    }


    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        name = Naming.mangleMemberName(name);
        signature = Naming.mangleFortressIdentifier(signature);
        desc = Naming.mangleMethodSignature(desc);

        return new ManglingMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
    }

    public MethodVisitor visitCGMethod(int access, String name, String desc, String signature, String[] exceptions) {

        return new ManglingMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
    }


    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        // We may need to do something interesting here.
        // Consider doing it pre-emptively rather than copping out on failure.
        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (Throwable e) {
            // [added try/catch wrapper to get useful information out on failure - JWM]
            // throw new Error("Couldn't getCommonSuperClass("+type1+", "+type2+")",e);

            // Note: the following apparent cop-out was gleaned by perusing:
            //   http://www.java2s.com/Open-Source/Java-Document/Byte-Code/asm/org/objectweb/asm/ClassWriterComputeFramesTest.java.htm
            // This returns java.lang.Object as the CommonSuperClass of anything involving an interface
            // type.  Since all Fortress traits correspond to interface types, we ought to be able to
            // do the same.
            //
            // That said, it still feels wrong.
            return Naming.javaObject;
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        signature = Naming.mangleFortressIdentifier(signature);
        name = Naming.mangleFortressIdentifier(name);
        superName = Naming.mangleFortressIdentifier(superName);

        String[] _interfaces =
            interfaces == null ? null : new String[interfaces.length];
        if (interfaces != null)
            for (int i = 0; i < interfaces.length; i++)
                _interfaces[i] = Naming.mangleFortressIdentifier(interfaces[i]);

        super.visit(version, access, name, signature, superName, _interfaces);
    }


    @Override
    public FieldVisitor visitField(int access, String name, String desc,
            String signature, Object value) {
        signature = Naming.mangleFortressIdentifier(signature);
        name = Naming.mangleMemberName(name);
        desc = Naming.mangleFortressDescriptor(desc);
        return super.visitField(access, name, desc, signature, value);
    }


    @Override
    public void visitInnerClass(String name, String outerName,
            String innerName, int access) {
        name = Naming.mangleFortressIdentifier(name);
        outerName = Naming.mangleFortressIdentifier(outerName);
        innerName = Naming.mangleFortressIdentifier(innerName);
        super.visitInnerClass(name, outerName, innerName, access);
    }


    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        name = Naming.mangleFortressIdentifier(name);
        owner = Naming.mangleFortressIdentifier(owner);
        desc = Naming.mangleFortressDescriptor(desc);
        super.visitOuterClass(owner, name, desc);
    }


    @Override
    public void visitSource(String file, String debug) {
        // TODO Auto-generated method stub
        super.visitSource(file, debug);
    }

}

