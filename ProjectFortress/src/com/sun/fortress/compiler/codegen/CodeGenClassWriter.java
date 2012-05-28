/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.codegen;

// This class allows us to wrap ClassWriters.
// It gives us the ability to turn bytecode debugging on and off.
// It also handles output to a jar.

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.ByteCodeWriter;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.Pair;

public class CodeGenClassWriter extends ManglingClassWriter {

    private final JarOutputStream jos;
    private final boolean isRoot;

    public CodeGenClassWriter(int flags, JarOutputStream jos) {
        super(flags);
        this.jos = jos;
        this.isRoot = true;
        if (jos==null) throw new Error("CodeGenClassWriter: null JarOutputStream to constructor");
    }

    public CodeGenClassWriter(int flags, CodeGenClassWriter parent) {
        super(flags);
        this.jos = parent==null ? null : parent.jos;
        this.isRoot = false;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        name = Naming.mangleMemberName(name);
        signature = Naming.mangleFortressIdentifier(signature);
        desc = Naming.mangleMethodSignature(desc);

        return new CodeGenMethodVisitor(access, name, desc, signature, exceptions,
                                        super.visitNoMangleMethod(access, name, desc, signature, exceptions));
    }

    public CodeGenMethodVisitor visitCGMethod(int access, String name, String desc, String signature, String[] exceptions) {
        name = Naming.mangleMemberName(name);
        signature = Naming.mangleFortressIdentifier(signature);
        desc = Naming.mangleMethodSignature(desc);

        return new CodeGenMethodVisitor(access, name, desc, signature, exceptions,
                                        super.visitNoMangleMethod(access, name, desc, signature, exceptions));
    }

    private String dumpClass0( String unmangledFileName ) {
        visitEnd();

        String file = Naming.mangleFortressIdentifier(unmangledFileName);
        byte [] bytes = toByteArray();
        if (ProjectProperties.getBoolean("fortress.bytecode.verify", false)) {
            PrintWriter pw = new PrintWriter(System.out);
            CheckClassAdapter.verify(new ClassReader(bytes), true, pw);
        }

        ByteCodeWriter.writeJarredClass(jos, file, bytes);
        return file;
    }

    private void dumpClass1() {
        if (!isRoot) return;
        try {
            jos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void dumpClass( String unmangledFileName ) {
        String file = dumpClass0(unmangledFileName);
        Debug.debug(Debug.Type.CODEGEN,1,"Wrote class ", file);
        dumpClass1();
    }

    public void dumpClass( String unmangledFileName, Naming.XlationData splist) {
        if (splist == null) {
            dumpClass(unmangledFileName);
        } else {
            String file = dumpClass0( unmangledFileName );
            ByteCodeWriter.writeJarredFile(jos, file, "xlation", splist.toBytes());
            Debug.debug(Debug.Type.CODEGEN,1,"Wrote generic class ", file);
            dumpClass1();
        }
    }

}
