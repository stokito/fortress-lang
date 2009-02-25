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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.Debug;

public class MyClassLoader extends ClassLoader {

    String repository = ProjectProperties.NATIVE_WRAPPER_CACHE_DIR + "/";

    public MyClassLoader() {
        // TODO Auto-generated constructor stub
    }

    public MyClassLoader(ClassLoader parent) {
        super(parent);
        // TODO Auto-generated constructor stub
    }

    public Class defineClass(String name, byte[] b) {
        return defineClass(name, b, 0, b.length);
    }
    public static String mangle(String s ) {
        return s;
    }

    @SuppressWarnings("unchecked")
    public void writeClass(String className, byte[] bytes) {
        String fileName = repository + className.replace('.', '/') + ".class";
        String directoryName = fileName.substring(0, fileName.lastIndexOf('/'));
        try {
            ProjectProperties.ensureDirectoryExists(directoryName);
            FileOutputStream out = new FileOutputStream(fileName);
            out.write(bytes);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
