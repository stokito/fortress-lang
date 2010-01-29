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

package com.sun.fortress.compiler;

import com.sun.fortress.repository.ProjectProperties;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class ByteCodeWriter {
    static Character dot = '.';
    static Character slash = '/';

    public static void writeJarredClass(JarOutputStream jos, String file, byte[] bytes) {
        writeJarredFile(jos, file, "class", bytes);
   
    }
    
    public static void writeJarredFile(JarOutputStream jos, String file, String suffix, byte[] bytes) {
        String fileName = file.replace(dot, slash) + "." + suffix;
        JarEntry ze = new JarEntry(fileName);
        try {
            ze.setSize(bytes.length);
            jos.putNextEntry(ze);
            jos.write(bytes);
            jos.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
   
    }
    
   public static void writeClass(String repository, String file, byte[] bytes) {
        String fileName = repository + file.replace(dot, slash) + ".class";
        writeClass(bytes, fileName);
    }

    public static void writeClass(byte[] bytes, String fileName) {
        String directoryName = fileName.substring(0, fileName.lastIndexOf(slash));
        try {
            ProjectProperties.ensureDirectoryExists(directoryName);
            FileOutputStream out = new FileOutputStream(fileName);
            out.write(bytes);
            out.flush();  // Is this implicit in close?
            out.close();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
