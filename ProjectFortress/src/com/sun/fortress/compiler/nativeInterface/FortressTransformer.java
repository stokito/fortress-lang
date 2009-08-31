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
import java.util.Map;
import java.util.Set;
import java.io.FileOutputStream;
import org.objectweb.asm.*;

import com.sun.fortress.compiler.ByteCodeWriter;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.phases.OverloadSet;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;
import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.useful.MultiMap;

public class FortressTransformer {
    static String repository = ProjectProperties.NATIVE_WRAPPER_CACHE_DIR + "/";


    @SuppressWarnings("unchecked")
    public static void transform(String inputClassName, 
            APIName api_name,
            Map<IdOrOpOrAnonymousName,MultiMap<Integer, Function>> size_partitioned_overloads,
            TypeAnalyzer ta) {
        try {
            ClassReader cr = new ClassReader(inputClassName);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            String outputClassName = Naming.NATIVE_PREFIX_DOT + inputClassName;
            
            FortressMethodAdapter fa = new FortressMethodAdapter(cw,
                    inputClassName, outputClassName, api_name, size_partitioned_overloads, ta);
            cr.accept(fa, 0);

            cw.visitEnd();
            byte[] b1 = cw.toByteArray();

            ByteCodeWriter.writeClass(repository, outputClassName, b1);
            
//            cr = new ClassReader(b1);
//            cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
//
//            FortressForeignAdapter ffa = new FortressForeignAdapter(cw,
//                    outputClassName, overloads);
//            cr.accept(ffa, 0);
//            cw.visitEnd();
//            byte[] b2 = cw.toByteArray();

//            ByteCodeWriter.writeClass(repository, outputClassName, b2);
        } catch (Throwable e) {
            e.printStackTrace();
            InterpreterBug.bug(e);
        }
    }


}
