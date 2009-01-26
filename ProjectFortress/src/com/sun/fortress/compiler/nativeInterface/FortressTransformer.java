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
import java.io.FileOutputStream;
import org.objectweb.asm.*;

public class FortressTransformer {
    @SuppressWarnings("unchecked")

    
        public static Class transform(MyClassLoader loader, String className) {
        Class result = null;
        try {
            ClassReader cr = new ClassReader(className);
            ClassWriter cw = new ClassWriter(1);
            FortressMethodAdapter fa = new FortressMethodAdapter(cw);
            cr.accept(fa, 0);
            byte[] b2 = cw.toByteArray();
            loader.writeClass(className, b2);
            Class c = loader.findClass(className);
            result = c;
             
        } catch (Throwable e) {
            e.printStackTrace();
        }  
        return result;
    }
        
}