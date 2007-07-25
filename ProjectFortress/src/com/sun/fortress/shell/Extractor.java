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

public class Extractor extends ShellObject {
   private static final String DEFAULT_FORTRESS_LOC = File.listRoots()[0] + "Fortress";

   // Keep a jar to extract into a fortress in the same directory as the class file for Extractor.
   private static final String JAR_LOC = "fortress.jar";

   public static void main(String[] args) throws IOException, InterruptedException {
      // Ensure command-line args are syntactically valid.
      if (args.length > 1) {
         System.err.println("Usage: Fortress_XXXX_XXXX_XXXX.jar <destination directory>");
         System.exit(-1);
      }

      // Determine directory to install fortress in.
      String fortressLoc;
      if (args.length == 1) { fortressLoc = canonicalize(args[0]); }
      else { fortressLoc = getPath(); }

      // Provide simple user instructions.
      printInstructions(fortressLoc);
      System.out.println();
      System.out.print("Extracting files from jar..");

      // Write the contents of the anthooks.xml and fortress.jar
      // (internal to the containing jar) as temp files.
      File build = Files.extractTemp("anthooks.xml", "build_", ".xml");
      printProgress();
      File jar = Files.extractTemp("fortress.jar", "extract_", ".jar");
      printProgress();

      // Use Ant to expand the extracted jar file into a fortress.
      try {
         Ant.run(build, "extract", "-DfortressLoc=" + fortressLoc, "-DjarLoc=" + jar.getAbsolutePath());
	 System.err.println("fortressLoc: " + fortressLoc);
	 System.err.println("build path: " + build.getAbsolutePath());
         Files.cp(build.getAbsolutePath(), fortressLoc + SEP + "Fortress" + SEP + "FORTRESS" + SEP + "bin" + SEP + "anthooks.xml");
      }
      finally {
         build.delete();
         jar.delete();
      }
   }

   private static String getPath() throws IOException {
      System.out.println("Please enter the name of the directory in which you'd like to install your fortress distribution");
      System.out.print("Or simply type Enter to install in the default directory (" + DEFAULT_FORTRESS_LOC + "): ");
      BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
      String path = input.readLine();
      if (path.equals("")) { path = DEFAULT_FORTRESS_LOC; }
      return canonicalize(path);
   }

   private static String canonicalize(String path) throws IOException {
      File file = new File(path);

      if (file.isAbsolute()) {
         return file.getCanonicalPath();
      }
      else { // We were given a relative path; prepend user's working directory to it.
         return new File(System.getProperty("user.dir") + SEP + path).getCanonicalPath();
      }
   }

   private static void printInstructions(String fortressLoc) {
      System.out.println();
      System.out.println
         ("A fortress distribution will be placed in the following directory:\n" +
          "     " + fortressLoc + SEP + "Fortress" + "\n" +
          "In order to use this fortress, take the following steps:\n" +
          "  -- In your shell's startup script, set an environment variable FORTRESS to:\n" +
          "     " + fortressLoc + SEP + "Fortress" + SEP + "FORTRESS \n" +
          "  -- Move the following Unix shell script file to a directory on your path:\n" +
          "     " + fortressLoc + SEP + "Fortress" + SEP + "bin" + SEP + "fortress\n" +
          "For more information on using this distribution, please refer to: \n" +
          "     " + fortressLoc + SEP + "Fortress" + SEP + "README.txt");
   }

   private static void printProgress() { System.out.print("."); }
}
