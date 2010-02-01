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

  
class ClassFileReader  {
    FileInputStream in;
    byte[] buf;
    int index;

    ClassFileReader(byte _buf[]) {
        buf = _buf;
        index = 0;
    }

    ClassFileReader (String fileName) {
        int i, j, k;
        String classpath = System.getProperty("java.class.path");
        char pathSeparator = System.getProperty("path.separator").charAt(0);
        String fileSeparator = System.getProperty("file.separator");

        System.out.println("CLASSPATH = "+classpath);
        i = 0; j = 0; k = classpath.length();
    
        j = classpath.indexOf(pathSeparator, 0);
        while (j != -1) {
            try {
                System.out.println("Trying to open file: "+classpath.substring(i,j)+fileSeparator+fileName);
                in = new FileInputStream(classpath.substring(i,j)+fileSeparator+
                                         fileName);
                buf = new byte[in.available()];
                in.read(buf);
                index = 0;
                return;
            }
            catch (Exception e) {
                i = j+1;
                j = classpath.indexOf(pathSeparator, i);
            }
        }
     
        try {
            System.out.println("Trying to open file: "+classpath.substring(i,k)+fileSeparator+fileName);
            in = new FileInputStream(classpath.substring(i,k)+fileSeparator+fileName);
            buf = new byte[in.available()];
            in.read(buf);
            index = 0;
            return;
        }
        catch (Exception f) {
            System.err.println("File Not Found: " + fileName);
        }
    }


    void close() {
        try {
            in.close();
        } catch (Exception e) {
            System.err.println("ClassFileReader  Close error");
        }
    }

    int read4Bytes() {
        return ((buf[index++]<<24) +
                ((buf[index++]<<16) & 0x00FF0000) +
                ((buf[index++]<<8) & 0x0000FF00) +
                ((buf[index++]) & 0x000000FF));
    }

    int read2Bytes() {
        return (((buf[index++]<<8) & 0x0000FF00) + 
                (buf[index++] & 0x000000FF));
    }

    int read1Byte() {
        return ((buf[index++] & 0x000000FF));
    }

    int available() {
        return (buf.length - index);
    }
  
    static boolean exists(String classFileName) {
        int i, j, k;
        String classpath = System.getProperty("java.class.path");
        char pathSeparator = System.getProperty("path.separator").charAt(0);
        String fileSeparator = System.getProperty("file.separator");
        i = 0; j = 0; k = classpath.length();
        File file;

        j = classpath.indexOf(pathSeparator, 0);
        while (j != -1) {
            file = new File(classpath.substring(i,j)+fileSeparator+classFileName);
            if (file.exists()) {
                return true;
            }
            else {
                i = j+1;
                j = classpath.indexOf(pathSeparator, i);
            }
        }

        file = new File(classpath.substring(i,k)+fileSeparator+classFileName);
        return file.exists();
    }
}


