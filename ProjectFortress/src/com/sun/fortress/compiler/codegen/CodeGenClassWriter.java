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

public class CodeGenClassWriter extends ClassWriter {

    public CodeGenClassWriter(int flags) {
        super(flags);
    }


    public CodeGenMethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new CodeGenMethodVisitor(access, name, desc, signature, exceptions, 
                                        super.visitMethod(access, name, desc, signature, exceptions));
    }

    // When we can reliably parse type descriptors, this will go away.

    public CodeGenMethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions, String[] argTypes, String resultType) {
        return new CodeGenMethodVisitor(access, name, desc, signature, exceptions, 
                                        super.visitMethod(access, name, desc, signature, exceptions),
                                        argTypes, resultType);
    }

}
            
            