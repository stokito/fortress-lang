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

package com.sun.fortress.tests.unit_tests;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.Shell;
import com.sun.fortress.interpreter.Driver;
import com.sun.fortress.interpreter.evaluator.Init;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.useful.StreamForwarder;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.WireTappedPrintStream;

import edu.rice.cs.plt.iter.IterUtil;

public class FileTests {

    public static class BaseTest extends TestCase {
        String f;
        String dir;
        String name;
        String path;

        /**
         * If true, only print test output for unexpected results.
         */
        boolean unexpectedOnly;
        boolean expectFailure;

        boolean printSuccess;
        boolean printFailure;

        public BaseTest(String path, String d, String s, boolean unexpected_only, boolean expect_failure) {
            super("testFile");
            this.f = d + "/" + s;
            this.dir = d;
            this.path = path;
            this.name = s;
            this.unexpectedOnly = unexpected_only;
            this.printSuccess = !unexpected_only || expect_failure;
            this.printFailure = !unexpected_only || !expect_failure;
            this.expectFailure = expect_failure;
        }
        
        public String getName() {
            return f;
        }
        
    }
    
    public static class ShellTest extends BaseTest {
        
        public ShellTest(String path, String d, String s, boolean unexpected_only, boolean expect_failure) {
            super(path, d, s, unexpected_only, s.startsWith("XXX") ? !expect_failure : expect_failure);
        }

        public void testFile() throws Throwable {
            String scriptName = f + ".sh";
            Runtime runtime = Runtime.getRuntime();
            System.out.print("  ") ; System.out.print(f); System.out.print(" "); System.out.flush();
            
            Process p = runtime.exec(scriptName);
            InputStream err = p.getErrorStream();
            InputStream out = p.getInputStream();
            OutputStream in = p.getOutputStream();
            in.close();
            
            ByteArrayOutputStream cached_err = new ByteArrayOutputStream();
            ByteArrayOutputStream cached_out = new ByteArrayOutputStream();
            
            StreamForwarder f_out = new StreamForwarder(out, cached_out, true);
            StreamForwarder f_err = new StreamForwarder(err, cached_err, true);
            
                f_out.join();
                f_err.join();
                int exitValue = p.waitFor();
                String s_out = cached_out.toString();
                String s_err = cached_err.toString();
                
                boolean fail_exit = exitValue != 0;
                // We've decided that exit codes are definitive.
                boolean fail_out =  false && s_out.contains("FAIL");
                boolean fail_err =  false && s_err.contains("FAIL");
                boolean failed = fail_exit || fail_out || fail_err;
                String fail_cause = !failed ? "" : (
                        (fail_exit ? "Exit code != 0" : "") +
                        (fail_out ? " Stdout contained FAIL" : "") +
                        (fail_err ? " Stderr contained FAIL" : "")
                ).trim();
                
                if (failed && printFailure || !failed && printSuccess) {
                    System.out.print(s_out);
                    System.out.print(s_err);
                }
                
                if (expectFailure != failed) {
                    System.out.println(failed ? "UNEXPECTED failure ("+ fail_cause+")" : "Did not see expected failure");
                    fail();
                } else {
                    System.out.println(failed ? "Saw expected failure ("+ fail_cause+")" : "Passed");
                }

           
            

        }
    }
    
    public static class FSSTest extends BaseTest {
        
        public FSSTest(String path, String d, String s, boolean unexpected_only, boolean expect_failure) {
            super(path, d, s, unexpected_only, expect_failure);
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
            String fssFile = f + ".fss";

            BufferedReader in = Useful.utf8BufferedFileReader(fssFile);
            long start = System.nanoTime();
            GraphRepository fr = Shell.specificRepository( ProjectProperties.SOURCE_PATH.prepend(path) );
            try {
                try {
                    oldOut.print("  ") ; oldOut.print(f); oldOut.print(" "); oldOut.flush();
                    APIName apiname = NodeFactory.makeAPIName(s);
                    ComponentIndex ci = fr.getLinkedComponent(apiname);

                    //Option<CompilationUnit> _p = ASTIO.parseToJavaAst(fssFile, in, false);

                    {
                        Component p = (Component) ci.ast();

                        // oldOut.print(" RUNNING"); oldOut.flush();
                        if (!unexpectedOnly) System.out.println();
                        // A demo file requiring arguments
                        if (name.equals("tennisRanking")) {
                            ArrayList<String> args = new ArrayList<String>();
                            args.add(dir + "/tennis050307");
                            args.add(dir + "/tennis051707");
                            args.add(dir + "/tennisGames");
                            Driver.runProgram(fr, ci, args);
                        }
                        // Test files requiring "test" command
                        else if (name.equals("XXXTestTest") ||
                                 name.equals("natInference0") ||
                                 name.equals("testTest1") ||
                                 name.equals("testTest2")) {
                            Driver.runTests(fr, ci, false);
                        }
                        else {
                            Driver.runProgram(fr, ci, new ArrayList<String>());
                        }
                    }
                }
                finally {
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
                        System.out.println(" UNEXPECTED exception ");
                        ex.printStackTrace();
                        fail();
                    } else {
                        System.out.println(" UNEXPECTED exception");
                        fail(ex.getMessage());
                    }
                }
            } finally {
                            //fr.clear();
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
                long duration = (System.nanoTime() - start) / 1000000;
                    System.out.println(anyFails ? " FAIL" : " OK (time = " + duration + "ms)");
                    wt_err.flush(anyFails ? printFailure : printSuccess);
                    wt_out.flush(anyFails ? printFailure : printSuccess);

                    assertFalse("Saw failure string", anyFails);

            }
        }

    }
    public static void main(String[] args) throws IOException {
        junit.textui.TestRunner.run(FileTests.suite("shelltests", true, false));
        junit.textui.TestRunner.run(FileTests.suite("tests", true, false));
        junit.textui.TestRunner.run(FileTests.suite("not_passing_yet", false, true));
    }

    public static TestSuite suite(String dir_name, boolean failsOnly, boolean expect_failure) throws IOException {
        TestSuite suite = new TestSuite("Runs all tests in " + dir_name) {
            public void run(TestResult result) {
                super.run(result);
                Init.allowForLeakChecks();
            }
        };
        String dirname = ProjectProperties.backslashToSlash(dir_name);
        File dir = new File(dirname);
        if (!dir.exists()) {
            System.err.println(dirname + " does not exist");
            throw new FileNotFoundException(dirname);
        }
        if (!dir.isDirectory()) {
            System.err.println(dirname + " exists but is not a directory");
            throw new IllegalArgumentException(dirname);
        }
        String[] files = dir.list();
        System.err.println(dir);
        Iterable<String> shuffled = IterUtil.shuffle(Arrays.asList(files));
        for(String s : shuffled){
              if (s.endsWith("Syntax.fss") || s.endsWith("DynamicSemantics.fss"))
                  System.out.println("Not compiling file " + s);
              else if (!s.startsWith(".")) {
                  if (s.endsWith(".fss")) {
                      int l = s.lastIndexOf(".fss");
                      String testname = s.substring(0, l);
                      suite.addTest(new FSSTest(dir.getCanonicalPath(), dirname, testname, failsOnly, expect_failure));
                  } else if (s.endsWith(".sh")) {
                      int l = s.lastIndexOf(".sh");
                      String testname = s.substring(0, l);
                      suite.addTest(new ShellTest(dir.getCanonicalPath(), dirname, testname, failsOnly, expect_failure));
                  } else {
                      System.out.println("Not compiling file " + s);
                  }
              }
        }
        return suite;
    }
}
