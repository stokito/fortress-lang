/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.runtimeSystem;

import com.sun.fortress.useful.Useful;

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
            Useful.ensureDirectoryExists(directoryName);
            FileOutputStream out = new FileOutputStream(fileName);
            out.write(bytes);
            out.flush();  // Is this implicit in close?
            out.close();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
