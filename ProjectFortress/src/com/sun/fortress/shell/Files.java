/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.shell;

import java.io.*;
//import org.apache.tools.ant.*;
//import org.apache.tools.ant.taskdefs.*;
//import org.apache.tools.ant.types.selectors.*;

/*
 * This convenience class provides a simple API for common file actions.
 */
public class Files {
   public static void rm(String name) { new File(name).delete(); }
   public static void mkdir(String name) { new File(name).mkdir(); }
   public static void mv(String src, String dest) { new File(src).renameTo(new File(dest)); }
   public static File[] ls(String name) { return new File(name).listFiles(); }

   public static void cp(String src, String dest) throws FileNotFoundException, IOException {
      FileInputStream input = new FileInputStream(new File(src));
      FileOutputStream output = new FileOutputStream(new File(dest));

      for (int next = input.read(); next != -1; next = input.read()) {
         output.write(next);
      }
   }

   /* Convenience method for creating a BufferedReader from a file name. */
   public static BufferedReader reader(String fileName) throws IOException {
      return new BufferedReader(new FileReader(fileName));
   }

   /* Convenience method for creating a BufferedReader from a file name. */
   public static BufferedWriter writer(String fileName) throws IOException {
      return new BufferedWriter(new FileWriter(fileName));
   }
}
