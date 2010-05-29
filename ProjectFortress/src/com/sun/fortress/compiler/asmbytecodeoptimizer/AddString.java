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
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import java.util.ArrayList;

public class AddString {
    public static void optimize(ByteCodeVisitor bcv) {

        // These strings should be pulled out in a naming file somewhere.

        ByteCodeMethodVisitor bcmv = (ByteCodeMethodVisitor) bcv.methodVisitors.get("main([Ljava/lang/String;)V");

        if (bcmv != null) {
            bcmv.insns.add(0, new FieldInsn("GETSTATIC", Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            bcmv.insns.add(1, new LdcInsn("LdcInsn", "Running Optimized Version"));
            bcmv.insns.add(2, new MethodInsn("INVOKEVIRTUAL", Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                                          "(Ljava/lang/String;)V"));
        }
    }
}
