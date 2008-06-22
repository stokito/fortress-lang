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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.interpreter.reader.Lex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Unprinter;
import com.sun.fortress.shell.BatchCachingRepository;
import com.sun.fortress.compiler.FortressRepository;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.WireTappedPrintStream;
import com.sun.fortress.useful.Debug;

public class FileTests {


    public static class FSSTest extends TestCase {
        String f;
        String dir;
        String name;
        String path;
        // FortressRepository fr;

        /**
         * If true, only print test output for unexpected results.
         */
        boolean unexpectedOnly;


        boolean printSuccess;
        boolean printFailure;

        public FSSTest(String path, String d, String s, boolean unexpected_only, boolean expect_failure) {
            super("testFile");
            this.f = d + "/" + s;
            this.dir = d;
            this.path = path;
            this.name = s;
            this.unexpectedOnly = unexpected_only;
            this.printSuccess = !unexpected_only || expect_failure;
            this.printFailure = !unexpected_only || !expect_failure;
            // this.fr = repository;
        }

        public String getName() {
            return f;
        }

        public void testFile() throws Throwable {
            // Usefull when a test is running forever
//            System.out.println(this.name);
//            System.out.flush();

            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;
            WireTappedPrintStream wt_err =
                WireTappedPrintStream.make(System.err, unexpectedOnly);
            WireTappedPrintStream wt_out =
                WireTappedPrintStream.make(System.out, unexpectedOnly);
            System.setErr(wt_err);
            System.setOut(wt_out);

            String s = f.replaceFirst(".*/", "");
            String tmpFile = "/tmp/" + s + ".ast";
            String fssFile = f + ".fss";

            BufferedReader in = Useful.utf8BufferedFileReader(fssFile);
            try {
                try {
                    oldOut.print("  ") ; oldOut.print(f); oldOut.print(" "); oldOut.flush();
                    APIName apiname = NodeFactory.makeAPIName(s);
                    FortressRepository fr = Driver.extendedRepository( path );
                    ComponentIndex ci = fr.getLinkedComponent(apiname);

                    //Option<CompilationUnit> _p = ASTIO.parseToJavaAst(fssFile, in, false);

                    {
                        CompilationUnit p = ci.ast();

                        // oldOut.print(" RUNNING"); oldOut.flush();
                        if (!unexpectedOnly) System.out.println();
                        // A demo file requiring arguments
                        if (name.equals("tennisRanking")) {
                            ArrayList<String> args = new ArrayList<String>();
                            args.add(dir + "/tennis050307");
                            args.add(dir + "/tennis051707");
                            args.add(dir + "/tennisGames");
                            Driver.runProgram(fr, p, false, false, args);
                        }
                        // Test files requiring "-test" flag
                        else if (name.equals("XXXTestTest") ||
                                 name.equals("natInference0") ||
                                 name.equals("testTest2")) {
                            Driver.runProgram(fr, p, true, false, new ArrayList<String>());
                        }
                        else {
                            Driver.runProgram(fr, p, false, false, new ArrayList<String>());
                        }
                    }
                }
                finally {
                    // fr = null;
                    System.setErr(oldErr);
                    System.setOut(oldOut);
                    in.close();
                }
            }
            catch (Throwable ex) {
                if (f.contains("XXX")) {
                    // "Failed", but correctly
                    // !unexpectedOnly || expectFailure
                    wt_err.flush(printSuccess);
                    wt_out.flush(printSuccess);
                    String exFirstLine = ex.toString();
                    int crLoc = exFirstLine.indexOf("\n");
                    if (crLoc == -1) crLoc = exFirstLine.length();
                    exFirstLine = exFirstLine.substring(0, crLoc);
                    System.out.println(" OK Saw expected exception");
                    return;
                }
                else {
                    // Failed, really
                    if (printFailure) System.out.println();
                    wt_err.flush(printFailure);
                    wt_out.flush(printFailure);
                    if (printFailure) {
                        System.out.println(" UNEXPECTED exception " + ex);
                        ex.printStackTrace();
                        throw ex;
                    } else {
                        System.out.println(" UNEXPECTED exception");
                        fail(ex.getMessage());                    }
                }
            }

            /* Come here IFF NO EXCEPTIONS, to analyze output */

            String outs = wt_out.getString();
            // String errs = wt_err.getString();

            if (f.contains("XXX")) {
                // NOTE expect to see this on STANDARD OUTPUT, not ERROR.
                if (outs.contains("fail") || outs.contains("FAIL")) {
                    wt_err.flush(printSuccess);
                    wt_out.flush(printSuccess);
                    // Saw a failure, that is good.
                    System.out.println(" Saw expected failure " );
                } else {
                    if (printFailure) System.out.println();
                    wt_err.flush(printFailure);
                    wt_out.flush(printFailure);
                    System.out.println(" Missing expected failure " );
                    // Expected exception, saw none.
                    fail("Expected failure or exception, saw none.");
                }
            } else {
                boolean anyFails = outs.contains("fail") || outs.contains("FAIL");
                    System.out.println(anyFails ? " FAIL" : " OK");
                    wt_err.flush(anyFails ? printFailure : printSuccess);
                    wt_out.flush(anyFails ? printFailure : printSuccess);

                    assertFalse("Saw failure string", anyFails);

            }
        }
    }

    public static class JSTTest extends TestCase {
        String s;
        String d;

        public JSTTest(String d, String s) {
            super("testFile");
            this.s = s;
            this.d = d;
        }

        public String getName() {
            return d + "/" + s;
        }

        public void testFile() throws Throwable {
            System.out.println(s + " test");
            BufferedReader br = new BufferedReader(new FileReader(d + "/" + s + ".tfs"));
            FortressRepository fr = Driver.defaultRepository();
            Lex lex = new Lex(br);
            Unprinter up = new Unprinter(lex);
            lex.name(); // Inhale opening parentheses
            CompilationUnit p = (CompilationUnit) up.readNode(lex.name());
            Driver.runProgram(fr, p, true, false, new ArrayList<String>());
        }

    }

    public static void main(String[] args) throws IOException {
        junit.textui.TestRunner.run(FileTests.suite("tests", true, false));
        junit.textui.TestRunner.run(FileTests.suite("not_passing_yet", false, true));
    }

    public static Test suite(String dirname, boolean failsOnly, boolean expect_failure) throws IOException {
        return suite(dirname, failsOnly, expect_failure, 0.0, 1.0);
    }

    public static Test suite(String dirname, boolean failsOnly, boolean expect_failure, double begin, double end) throws IOException {
        TestSuite suite = new TestSuite("Test for default package");
        // $JUnit-BEGIN$
        dirname = ProjectProperties.backslashToSlash(dirname);
        File dir = new File(dirname);
        String[] files = dir.list();
        System.err.println(dir);

        /*
        FortressRepository fr =
            Driver.extendedRepository(dir.getCanonicalPath());
            */

//            ProjectProperties.noStaticAnalysis ?
//                    new BatchCachingRepository(
//                          //new PathBasedSyntaxTransformingRepository
//                          (ProjectProperties.SOURCE_PATH.prepend(dir)),
//                          new CacheBasedRepository(ProjectProperties.ensureDirectoryExists("./.interpreter_cache"))
//                          )
//                    :
//            new BatchCachingAnalyzingRepository(false,
//                (ProjectProperties.SOURCE_PATH.prepend(dir)),
//                new CacheBasedRepository(ProjectProperties.ensureDirectoryExists("./.analyzed_cache"))
//                );


        // fr.addRootApis();

        for (int i = 0; i < files.length; i++) {
            double f = i / (double) files.length;
            if (f < begin || f >= end)
                continue;
            String s = files[i];
            if (s.endsWith("Syntax.fss") || s.endsWith("DynamicSemantics.fss"))
                System.out.println("Not compiling file " + s);
            else if (!s.startsWith(".")) {
                if (s.endsWith(".fss")) {
                    int l = s.lastIndexOf(".fss");
                    //System.err.println("Adding " + s);
                    String testname = s.substring(0, l);
                    suite.addTest(new FSSTest(dir.getCanonicalPath(), dirname, testname, failsOnly, expect_failure));
                } else if (s.endsWith(".tfs")) {
                    int l = s.lastIndexOf(".tfs");
                    suite.addTest(new JSTTest(dirname, s.substring(0, l)));
                } else {
                    System.out.println("Not compiling file " + s);
                }
            }

        }
        // $JUnit-END$
        return suite;
    }
}
