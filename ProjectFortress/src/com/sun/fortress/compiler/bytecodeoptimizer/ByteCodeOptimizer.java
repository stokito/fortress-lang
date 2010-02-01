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
package com.sun.fortress.compiler.bytecodeoptimizer;
import java.io.*;
import java.util.ArrayList;
import java.util.jar.*;
import java.util.List;

class ByteCodeOptimizer {

    List<ClassToBeOptimized> classes = new ArrayList<ClassToBeOptimized>();    
    
    void readInClasses(String jarFile) {
        try {
            JarInputStream jario = new JarInputStream(new FileInputStream(jarFile));
            // How big can classes get?  10,000 is our limit for now.
            int bufsize = 10000;
            int bufchunk = 1000;
            JarEntry entry;
            int bytesread;

            while ((entry = jario.getNextJarEntry()) != null) {
                byte buf[] = new byte[bufsize];
                byte tempbuf[] = new byte[bufchunk];
                int bytepos = 0;
                int lastread = 0;

                while ((bytesread = jario.read(tempbuf, lastread, bufchunk)) > 0) {
                    System.arraycopy(tempbuf, 0, buf, bytepos, bytesread);
                    bytepos = bytepos + bytesread;
                }

                ClassToBeOptimized cls = new ClassToBeOptimized(entry.getName(), buf);
                cls.Print();
                classes.add(cls);
            }
        } catch (Exception e) {
            System.out.println("ClassFormatError:"  + e );
            e.printStackTrace();
        }

    }

    public static void main(String[] args) 
    {
        ByteCodeOptimizer bco = new ByteCodeOptimizer();
        bco.readInClasses(args[0]);
        //        bco.writeOutClasses(args[0]);
    }

}

	
