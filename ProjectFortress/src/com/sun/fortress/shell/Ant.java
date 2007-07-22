/********************************************************************************
    Copyright 2006 Sun Microsystems, Inc., 
    4150 Network Circle, Santa Clara, California 95054, U.S.A. 
    All rights reserved.

    U.S. Government Rights - Commercial software. 
    Government users are subject to the Sun Microsystems, Inc. standard 
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered 
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
********************************************************************************/

package com.sun.fortress.shell;

import java.io.*;
import java.util.*;
//import org.apache.tools.ant.*;
//import org.apache.tools.ant.launch.Launcher;

/* 
 * Class for handling dispatches to the Ant targets kept in anthooks.xml.
 */
public final class Ant extends ShellObject {
   
   /* 
    * Invoke a new JVM to run the main method of class Launcher, which invokes Ant.
    * A separate JVM is invoked because Ant manipulates JVM state and terminates the 
    * JVM when finished.
    */
   public static int run(File buildFile, String... args) throws InterruptedException {
      ArrayList<String> argsList = new ArrayList<String>();
      // Type erasure workaround: We need to construct a result array and pass it to toArray 
      // (below) in order to get back a String[] and not an Object[]. 
      String[] result = new String[args.length + 6];
      
      argsList.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
      argsList.add("-classpath");
      argsList.add(System.getProperty("java.class.path"));
      argsList.add("org.apache.tools.ant.launch.Launcher");
      argsList.add("-buildfile");
      argsList.add(buildFile.getAbsolutePath());
      argsList.add("-q");
      for (String arg : args) { argsList.add(arg); }
                         
      ProcessBuilder antProcessBuilder = new ProcessBuilder(argsList.toArray(result));
      antProcessBuilder.directory(new File(System.getProperty("user.dir")));

      int exitValue = -1; // Expect the worst.
      try {
         Process process = antProcessBuilder.start();
         process.waitFor();
 
         InputStream input = new BufferedInputStream(process.getInputStream());
         InputStream error = new BufferedInputStream(process.getErrorStream());
         
         for (int next = input.read(); next != -1; next = input.read()) {
            System.out.write(next);
         }
         for (int next = error.read(); next != -1; next = error.read()) {
            System.out.write(next);
         }
         System.out.flush();
         exitValue = process.exitValue();
      }
      catch (IOException e) {
         System.err.println("Error when attempting to invoke a separate Java process: ");
         System.err.println(e.getMessage());
         System.err.println("Please be sure that Java is installed properly and its scripts are available " +
                            "from the command line.");
      }

      return exitValue;
   }
   
   public static int run(String... args) throws InterruptedException { 
      
      return run(new File(FORTRESS + SEP + "bin" + SEP + "anthooks.xml"), args); 
   }
}

