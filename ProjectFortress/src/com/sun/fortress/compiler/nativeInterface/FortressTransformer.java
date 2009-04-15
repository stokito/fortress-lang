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

import java.lang.reflect.Method;
import java.util.Set;
import java.io.FileOutputStream;
import org.objectweb.asm.*;
import com.sun.fortress.compiler.ByteCodeWriter;
import com.sun.fortress.compiler.phases.OverloadSet;
import com.sun.fortress.repository.ProjectProperties;

public class FortressTransformer {
    static String repository = ProjectProperties.NATIVE_WRAPPER_CACHE_DIR + "/";


    @SuppressWarnings("unchecked")
    public static void transform(String inputClassName, Set<OverloadSet> overloads) {
        String outputClassName;
        try {
            ClassReader cr = new ClassReader(inputClassName);
            ClassWriter cw = new ClassWriter(1);
            FortressMethodAdapter fa = new FortressMethodAdapter(cw, inputClassName, overloads);
            cr.accept(fa, 0);
            byte[] b2 = cw.toByteArray();
            ByteCodeWriter.writeClass(repository, inputClassName, b2);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
