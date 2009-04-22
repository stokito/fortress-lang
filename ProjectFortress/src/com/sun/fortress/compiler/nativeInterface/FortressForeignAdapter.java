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

package com.sun.fortress.compiler.nativeInterface;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.sun.fortress.compiler.phases.OverloadSet;
import com.sun.fortress.useful.Debug;

public class FortressForeignAdapter extends ClassAdapter {
    private final Set<OverloadSet> overloads;
    HashSet<String> overloadsDone = new HashSet<String>();
    String className;
    
    public FortressForeignAdapter(ClassVisitor cv,
            String outputClassName,
            Set<OverloadSet> overloads) {
        super(cv);
        this.overloads = overloads;
        className = outputClassName.replace('.','/');
    }

    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        Debug.debug( Debug.Type.COMPILER, 1,
                     "visit:" + name + " generate " + className);
        cv.visit(version, access, className, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        // Don't know how to do these, or if we need them...
        if (name.equals("<init>") || name.equals("<clinit>"))
            Debug.debug(Debug.Type.COMPILER, 1, "Don't visit Method " + name);
        else if (SignatureParser.unsayable(desc))
            Debug.debug(Debug.Type.COMPILER, 1,
                    "Don't visit Method with unsayable desc" + name);
        else {
                      
            if (!overloadsDone.contains(name)) {
                // Generate all the overloadings.
                overloadsDone.add(name);
                for (OverloadSet o : overloads) {
                    String oname = o.getName().stringName();
                    oname = oname.substring(oname.lastIndexOf(".") + 1);
                    if (oname.equals(name)) {
                        generateAnOverload(oname, cv, o);
                    }
                }
            } 
            

        }

        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    private static void generateAnOverload(String name, ClassVisitor cv,
            OverloadSet o) {

        // "(" anOverloadedArg^N ")" returnType
        // Not sure what to do with return type.
        String signature = o.getSignature();
        String[] exceptions = o.getExceptions();
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC
                + Opcodes.ACC_STATIC, // access,
                name, // name,
                signature, // sp.getFortressifiedSignature(),
                null, // signature, // depends on generics, I think
                exceptions); // exceptions);

        mv.visitCode();
        Label fail = new Label();

        o.generateCall(mv, 0, fail); // Guts of overloaded method

        // Emit failure case
        mv.visitLabel(fail);
        // Boilerplate for throwing an error.
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/Error");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("Should not happen");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Error", "<init>",
                "(Ljava/lang/String;)V");
        mv.visitInsn(Opcodes.ATHROW);

        mv.visitMaxs(o.getParamCount(), o.getParamCount()); // autocomputed
        mv.visitEnd();

    }

}
