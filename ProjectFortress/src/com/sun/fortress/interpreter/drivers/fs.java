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

package com.sun.fortress.interpreter.drivers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.interpreter.env.FortressTests;
import com.sun.fortress.interpreter.evaluator.Init;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.nodes.CompilationUnit;
import com.sun.fortress.interpreter.nodes.Printer;
import com.sun.fortress.interpreter.useful.Useful;
import com.sun.fortress.interpreter.typechecker.TypeChecker;
import com.sun.fortress.interpreter.typechecker.TypeError;

public class fs {

    static final String SXP_SUFFIX = ".sxp";

    static final String JAVA_AST_SUFFIX = ".tfs";

    static String timeStamp;

    static boolean doAst = false;

    static boolean runTests = false;

    static String basename(String s) {
        // Note that this is perhaps a no-op, so the
        // old Windows-aware code must remain.
        s = ProjectProperties.backslashToSlash(s);
        
        int sepi1 = s.lastIndexOf("/");
        // Why look twice? On Windows, either separator works!
        // Write Once, Paranoid Always
        int sepi2 = s.lastIndexOf(File.separator);
        int sepi = Math.max(sepi1, sepi2); // Handles pathologies and choice

        if (sepi >= 0) {
            s = s.substring(sepi + 1, s.length());
        }

        int doti = s.lastIndexOf(".");

        if (doti > 0) { // We don't like empty file names, hence > 0
            s = s.substring(0, doti);
        }
        return s;
    }

    /**
     * Reads in the O-Caml-com.sun.fortress.interpreter.parser-format S-expression file in tmpFile, and
     * attempts to run it. If the result of the execution is not void, (attempt
     * to) print it.
     *
     * @param tmpFile
     * @throws Throwable
     */
    public static void runInterpreter(CompilationUnit p,
            boolean runTests, List<String> args) throws Throwable {
        long begin = System.currentTimeMillis();
        Driver.runProgram(p, runTests, libraryTest, args);
        System.err.println("" + (System.currentTimeMillis() - begin)
                + " milliseconds");
    }
    
    public static boolean check(CompilationUnit p) {
        try { TypeChecker.check(p); return true; }
        catch (TypeError e) {
            System.err.println("Static error: " + e);
            return false;
        }
    }

    static volatile CompilationUnit p;
    static boolean verbose = false;
    static boolean keep = false;
    static boolean tryRats = false;
    static String explicitParser = null;
    static boolean parseOnly = false;
    static boolean checkOnly = false;
    static boolean libraryTest = false;
    static boolean test = false;
    static boolean pause = false;
    static List<String> listArgs = new ArrayList<String>();

    /**
     * For each input file, parse it to create an S-expression tmpfile, then
     * interpret the contents of hte tmpfile, and if there were no errors along
     * the way, delete the tmpfile.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            usage();
        }
        String s = null;

        for (int i = 0; i < args.length; i++) {
            s = args[i];
            if (s.startsWith("-")) {
                if ("-ast".equalsIgnoreCase(s)) {
                    doAst = true;
                } else if ("-keep".equalsIgnoreCase(s)) {
                    keep = true;
                } else if ("-pause".equalsIgnoreCase(s)) {
                    pause = true;
                } else if ("-parseOnly".equalsIgnoreCase(s)) {
                  parseOnly = true;
                } else if ("-checkOnly".equalsIgnoreCase(s)) {
                  checkOnly = true;
                } else if ("-libraryTest".equalsIgnoreCase(s)) {
                  libraryTest = true;
                } else if ("-v".equalsIgnoreCase(s)) {
                    verbose = true;
                } else if ("-t".equalsIgnoreCase(s)) {
                    test = true;
                } else {
                    System.err.println("Unexpected option " + s);
                    usage();
                    System.exit(1);
                }
            } else {
                // Got what seems to be a file name...
                i++;
                // Collect Fortress parameters.
                while (i < args.length) {
                    listArgs.add(args[i]);
                    i++;
                }
                break;
            }
            s = null;
        }

        //

        if (s == null) {
            System.err.println("No file to run");
            usage();
            System.exit(1);
        } else {
            parseAndRun(s);
        }
        if (pause) {
            FortressTests.reset();
            Init.initializeEverything();

            System.gc();
            System.out.println("Type 'return' to exit");
            int i = System.in.read();
            while (i != -1 && i == '\n') {
                i = System.in.read();
            }
        }
    }

    /**
     * @param s
     */
    private static void parseAndRun(String s) {
      timeStamp = Useful.timeStamp();
      
//        String tmpFile = System.getProperty("java.io.tmpdir") + "/"
//                + basename(s) + "." + timeStamp + SXP_SUFFIX;
//        tmpFile = ProjectProperties.backslashToSlash(tmpFile);
//        boolean keepTemp = keep;
      
      try {
        
        if (verbose) System.err.println("Parsing " + s + " with Rats!");
        long begin = System.currentTimeMillis();
        p = Driver.parseToJavaAst(s, Useful.utf8BufferedFileReader(s));
        System.err.println("Parsing " + s + " with the Rats! parser: "
                             + (System.currentTimeMillis() - begin)
                             + " milliseconds");
        
        
        if (doAst) {
          String astFile = basename(s) + JAVA_AST_SUFFIX;
          if (verbose)
            System.err.println("Writing ast to " + astFile);
          
          BufferedWriter fout = Useful
            .utf8BufferedFileWriter(astFile);
          Appendable out = fout;
          
          (new Printer(true, true, true)).dump(p, out, 0);
          if (fout != null)
            fout.close();
        }
        if (test) {
          if (verbose)
            System.err.println("Running Tests");
          runTests = true;
        }
        
        if (parseOnly) {
          /* Skip interpreting. */
        } else if (p==null) {
          /* Indicate an error occurred. */
          System.err.println("FAIL: Syntax error.");
        } else {
          if (verbose)
            System.err.println("Checking");
          
          boolean typesafe = check(p);
          
          if (typesafe && !checkOnly) {
            
            if (verbose)
              System.err.println("Interpreting");
            
            runInterpreter(p, runTests, listArgs);
            p = null;
          }
        }
        
      } catch (Throwable th) {
        synchronized (Throwable.class) {
          // keepTemp = true;
          th.printStackTrace();
          if (th instanceof ProgramError) {
            System.out
              .println("\n--------Fortress error appears below--------\n");
            ((ProgramError) th).printInterpreterStackTrace(System.out);
            System.out.println();
            System.out.println(th.getMessage());
          }
        }
      }
//        if (!keepTemp) {
//            // (new File(tmpFile)).delete();
//        }
    }

    static void usage() {
        System.err
                .println("Usage: java fs  [-v] [-ast] [-pause] [-parseOnly] Fortress-source-file-name Fortress-String-args");
        System.err
                .println("Iteratively parses and executes a Fortress source file.");
        System.err.println("The -ast option writes out the ast.");
        System.err.println("The -pause option performs a reset and gc after running the program, and then waits for input.");
        System.err.println("The -parseOnly option parses but does not evaluate the program.");
        System.err.println("The -v option is more verbose.");
    }

}
