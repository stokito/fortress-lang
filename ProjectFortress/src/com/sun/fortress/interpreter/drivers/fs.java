/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.interpreter.env.FortressTests;
import com.sun.fortress.interpreter.evaluator.Init;
import com.sun.fortress.interpreter.evaluator.FortressError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Printer;
import com.sun.fortress.shell.BatchCachingRepository;
import com.sun.fortress.useful.Useful;

public class fs {

  
    // Options set by main()
    private static boolean doAst = false;
    private static boolean verbose = false;
    private static boolean keep = false;
    private static boolean tryRats = false;
    private static String explicitParser = null;
    private static boolean parseOnly = false;
    private static boolean libraryTest = false;
    private static boolean woLibrary = false;
    private static boolean test = false;
    private static boolean pause = false;
    private static List<String> listArgs = new ArrayList<String>();
    private static boolean verboseExceptions = false;

    private static String basename(String s) {
        String filename = new File(s).getName();
        int doti = filename.lastIndexOf(".");
        if (doti > 0) { // We don't like empty file names, hence > 0
            return filename.substring(0, doti);
        }
        else { return filename; }
    }

    public static void main(String[] args) throws Throwable {

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
                } else if ("-libraryTest".equalsIgnoreCase(s)) {
                  libraryTest = true;
                } else if ("-nolib".equalsIgnoreCase(s)) {
                  woLibrary = true;
                } else if ("-v".equalsIgnoreCase(s)) {
                    verbose = true;
                } else if ("-t".equalsIgnoreCase(s)) {
                    test = true;
                } else if ("-debug".equalsIgnoreCase(s)) {
                    verboseExceptions = true;
                } else {
                    System.err.println("Unexpected option " + s);
                    usage();
                    System.exit(1);
                }
            }
            else {
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

        if (s == null) {
            System.err.println("No file to run");
            usage();
            System.exit(1);
        }
        else {
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

    private static void reportCompletion(String task, long beginTime) {
        long time = System.currentTimeMillis() - beginTime;
        System.err.println(task + ": " + time + " milliseconds");
    }

    /**
     * Parse and run the given program. Returns true iff there were no errors.
     */
    private static void parseAndRun(String s) throws Throwable {
//        String timeStamp = Useful.timeStamp();

//        String tmpFile = System.getProperty("java.io.tmpdir") + "/"
//                + basename(s) + "." + timeStamp + SXP_SUFFIX;
//        tmpFile = ProjectProperties.backslashToSlash(tmpFile);
//        boolean keepTemp = keep;

        try {
            BatchCachingRepository fr;
            // Need to replace \ with / in the path.
            if (s.contains("/")) {
                String head = s.substring(0, s.lastIndexOf("/"));
                s = s.substring(s.lastIndexOf("/")+1, s.length());
                fr = Driver.extendedRepository(head);
            } else {
                fr = Driver.defaultRepository();
            }
            
            fr.addRootApis(NodeFactory.makeAPIName("FortressBuiltin"));
            fr.addRootApis(NodeFactory.makeAPIName("FortressLibrary"));
            
            if (verbose)  { System.err.println("Parsing/reading " + s); }
            long begin = System.currentTimeMillis();
            
            Option<CompilationUnit> p = Option.none();
            
            try {
                APIName name;
                name = Driver.fileAsApi(s);
                
                if (name != null) {
                    p = Option.wrap(fr.getApi(name).ast());
                } else {
                    name = Driver.fileAsComponent(s);
                    if (name != null) {
                        p = Option.wrap(fr.getLinkedComponent(name).ast());
                    } else {
                        name = NodeFactory.makeAPIName(s);
                        p = Option.wrap(fr.getLinkedComponent(name).ast());
                    }
                } 
                reportCompletion("Parsing " + s, begin);
            }
            finally { }

            if (doAst && p.isSome()) {
                String astFile = basename(s) + "." + ProjectProperties.astSuffixForSource(s);
                if (verbose) { System.err.println("Writing ast to " + astFile); }
                BufferedWriter fout = Useful.utf8BufferedFileWriter(astFile);
                try { new Printer(true, true, true).dump(Option.unwrap(p), fout, 0); }
                finally { fout.close(); }
            }

            if (p.isNone()) {
                System.err.println("FAIL: Syntax error(s).");
                System.exit(1);
            }
            else if (!parseOnly) {
                CompilationUnit _p = Option.unwrap(p);

                if (verbose) {
                  if (test) { System.err.println("Running Tests"); }
                  System.err.println("Interpreting");
                }
                begin = System.currentTimeMillis();
                Driver.runProgram(fr, _p, test, libraryTest, woLibrary, listArgs);
                reportCompletion("Program execution", begin);
            }

        }
        catch (FortressError e) {
//            keepTemp = true;
            System.err.println("\n--------Fortress error appears below--------\n");
            if (verboseExceptions) {
                e.printStackTrace();
            } else {
                e.printInterpreterStackTrace(System.err);
                System.err.println();
                System.err.println(e.getMessage());
                System.err.println("Turn on -debug for Java-level error dump.");
            }
            System.exit(1);
        }
//        catch (Throwable th) {
//            keepTemp = true;
//            th.printStackTrace();
//        }
//        if (!keepTemp) {
//            (new File(tmpFile)).delete();
//        }
    }
    
    static void usage() {
        System.err.println("Usage: java fs flags* filename run-args");
        System.err.println("Iteratively parses the Fortress source file found in filename and executes it using run-args.");
        System.err.println("Flag summary:");
        System.err.println(" -ast   writes out the ast.");
        System.err.println(" -pause   performs a reset and gc after running the program, and then waits for input.");
        System.err.println(" -parseOnly   parses but does not evaluate the program.");
        System.err.println(" -keep   keeps some intermediate files.");
        System.err.println(" -v   verbose parsing.");
        System.err.println(" -debug   show a full Java stack trace if execution fails.");
    }

}
