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

// This class allows us to wrap ClassWriters.
// It gives us the ability to turn bytecode debugging on and off.

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

import com.sun.fortress.runtimeSystem.Naming;

public class CodeGenClassWriter extends ManglingClassWriter {

    public CodeGenClassWriter(int flags) {
        super(flags);
    }


    
    
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        name = Naming.mangleMemberName(name);
        signature = Naming.mangleFortressIdentifier(signature);
        desc = Naming.mangleMethodSignature(desc);

        return new CodeGenMethodVisitor(access, name, desc, signature, exceptions, 
                                        super.visitCGMethod(access, name, desc, signature, exceptions));
    }
    
    public CodeGenMethodVisitor visitCGMethod(int access, String name, String desc, String signature, String[] exceptions) {
        name = Naming.mangleMemberName(name);
        signature = Naming.mangleFortressIdentifier(signature);
        desc = Naming.mangleMethodSignature(desc);

        return new CodeGenMethodVisitor(access, name, desc, signature, exceptions, 
                                        super.visitCGMethod(access, name, desc, signature, exceptions));
    }

}
            
            