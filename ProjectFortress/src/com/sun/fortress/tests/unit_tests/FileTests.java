/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
import java.util.List;
import java.util.Map;
import java.util.Random;

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
import com.sun.fortress.useful.StringMap;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.WireTappedPrintStream;

import edu.rice.cs.plt.iter.IterUtil;

public class FileTests {

    public static class BaseTest extends TestCase {
        /**
         * Directory-qualified file name
         */
        String f;
        /**
         * Just the directory.
         */
        String dir;
        /**
         * The name of the test.
         */
        String name;
        /**
         * The search path for analysis, compilation, etc.
         */
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

    /**
     * For tests that are applied to a single source file (or are rooted at a
     * single source file), this takes care of checking for success/failure,
     * what is expected, printing, etc.
     */
    public abstract static class SourceFileTest extends BaseTest {
        public SourceFileTest(String path, String d, String s,
                boolean unexpected_only, boolean expect_failure) {
            super(path, d, s, unexpected_only, expect_failure);
        }

        public abstract String tag();
        
        public void testFile() throws Throwable {
            // Useful when a test is running forever
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

            
            long start = System.nanoTime();
            String fssFile = f + ".fss";
            int rc = 0;
            
            try {
                BufferedReader in = null;
                try {
                    oldOut.print(" " + tag() + " ") ; oldOut.print(f); oldOut.print(" "); oldOut.flush();
                    in = Useful.utf8BufferedFileReader(fssFile);
 
                    rc = justTheTest();
                }
                finally {
                    System.setErr(oldErr);
                    System.setOut(oldOut);
                    if (in != null)
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
            String errs = wt_err.getString();
            boolean anyFails = outs.contains("fail") || outs.contains("FAIL") ||
            
            errs.contains("fail") || errs.contains("FAIL") || rc != 0;

            if (f.contains("XXX")) {
                // NOTE expect to see this on STANDARD OUTPUT, not ERROR.
                if (anyFails) {
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
                long duration = (System.nanoTime() - start) / 1000000;
                    System.out.println(anyFails ? " FAIL" : " OK (time = " + duration + "ms)");
                    wt_err.flush(anyFails ? printFailure : printSuccess);
                    wt_out.flush(anyFails ? printFailure : printSuccess);

                    assertFalse("Saw failure string", anyFails);

            }
        }

        abstract protected int justTheTest() throws FileNotFoundException, IOException, Throwable;

    }
    
    public static class TestTest extends BaseTest {

        private final StringMap props;
        
        public TestTest(StringMap props, String path, String d, String s,
                boolean unexpected_only, boolean expect_failure) {
            super(path, d, s, unexpected_only, expect_failure);
            this.props = props;
        }
        
        public void testFile() throws Throwable {
            String scriptName = ProjectProperties.FORTRESS_AUTOHOME + "/bin/run";
            Runtime runtime = Runtime.getRuntime();
            System.out.print(" run  ") ; System.out.print(f); System.out.print(" "); System.out.flush();

            ProcessBuilder pb = new ProcessBuilder(scriptName, name);
            Map<String, String> env = pb.environment();
            if (! env.containsKey("FORTRESS_HOME")) {
                env.put("FORTRESS_HOME", ProjectProperties.FORTRESS_AUTOHOME);
            }
            Process p = pb.start();
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
        
    
    
    public static class ShellTest extends BaseTest {

        public ShellTest(String path, String d, String s, boolean unexpected_only, boolean expect_failure) {
            super(path, d, s, unexpected_only, s.startsWith("XXX") ? !expect_failure : expect_failure);
        }

        public void testFile() throws Throwable {
            String scriptName = f + ".sh";
            Runtime runtime = Runtime.getRuntime();
            System.out.print("  ") ; System.out.print(f); System.out.print(" "); System.out.flush();

            ProcessBuilder pb = new ProcessBuilder(scriptName);
            Map<String, String> env = pb.environment();
            if (! env.containsKey("FORTRESS_HOME")) {
                env.put("FORTRESS_HOME", ProjectProperties.FORTRESS_AUTOHOME);
            }
            Process p = pb.start();
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
    
    public static class CompileTest extends SourceFileTest {

        public CompileTest(String path, String d, String s,
                boolean unexpected_only, boolean expect_failure) {
            super(path, d, s, unexpected_only, expect_failure);
        }

        @Override
        protected int justTheTest()
                throws FileNotFoundException, IOException, Throwable {
            String[] tokens = {"compile", dir+"/"+name+".fss"};
            int rc = com.sun.fortress.Shell.subMain(tokens);
            return rc;
            
        }

        @Override
        public String tag() {
            // TODO Auto-generated method stub
            return "compile";
        }
        
    }

    public static class LinkTest extends SourceFileTest {

        public LinkTest(String path, String d, String s,
                boolean unexpected_only, boolean expect_failure) {
            super(path, d, s, unexpected_only, expect_failure);
        }

        @Override
        protected int justTheTest()
                throws FileNotFoundException, IOException, Throwable {
            // might need to strip the .fss off f "f".
            String[] tokens = {"link", dir+"/"+name+".fss"};
            int rc = com.sun.fortress.Shell.subMain(tokens);
            return rc;
            
            
        }

        @Override
        public String tag() {
            // TODO Auto-generated method stub
            return "link";
        }
        
    }

    public static class InterpreterTest extends SourceFileTest {

        public InterpreterTest(String path, String d, String s, boolean unexpected_only, boolean expect_failure) {
            super(path, d, s, unexpected_only, expect_failure);
        }

        
        /**
         * @param repository
         * @param apiname
         * @throws FileNotFoundException
         * @throws IOException
         * @throws Throwable
         */
        protected int justTheTest()
                throws FileNotFoundException, IOException, Throwable {
            String s = f.replaceFirst(".*/", "");
            APIName apiname = NodeFactory.makeAPIName(NodeFactory.testSpan, s);
            GraphRepository repository = Shell.specificRepository( ProjectProperties.SOURCE_PATH.prepend(path) );

            ComponentIndex ci = repository.getLinkedComponent(apiname);

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
                    Driver.runProgram(repository, ci, args);
                }
                // Test files requiring "test" command
                else if (name.equals("XXXTestTest") ||
                         name.equals("natInference0") ||
                         name.equals("testTest1") ||
                         name.equals("testTest2")) {
                    Driver.runTests(repository, ci, false);
                }
                else {
                    Driver.runProgram(repository, ci, new ArrayList<String>());
                }
            }
            return 0;
        }


        @Override
        public String tag() {
            // TODO Auto-generated method stub
            return "interpret";
        }

    }
    public static void main(String[] args) throws IOException {
        junit.textui.TestRunner.run(FileTests.interpreterSuite("shelltests", true, false));
        junit.textui.TestRunner.run(FileTests.interpreterSuite("tests", true, false));
        junit.textui.TestRunner.run(FileTests.interpreterSuite("not_passing_yet", false, true));
    }

    public static TestSuite interpreterSuite(
            String dir_name,
            boolean failsOnly,
            boolean expect_failure) throws IOException {
        
        TestSuite suite = new TestSuite("Runs all tests in " + dir_name) {
            public void run(TestResult result) {
                super.run(result);
                Init.allowForLeakChecks();
            }
        };
        
        String dirname = ProjectProperties.backslashToSlash(dir_name);
        File dir = directoryAsFile(dirname);
        System.err.println(dir);
        
        /* Many lines of random number generator seed nonsense. */
        
        Iterable<String> shuffled = shuffledFileList(dir);

        int testCount = testCount();
        int i = testCount;
        
        for(String s : shuffled){
              if (i <= 0) {
                  System.out.println("Early testing exit after " + testCount + " tests");
                  break;
              }
              boolean decrement = true;
              if (s.endsWith("Syntax.fss") || s.endsWith("DynamicSemantics.fss")) {
                  System.out.println("Not compiling file " + s);
                  decrement = false;
              } else if (!s.startsWith(".")) {
                  if (s.endsWith(".fss")) {
                      int l = s.lastIndexOf(".fss");
                      String testname = s.substring(0, l);
                      suite.addTest(new InterpreterTest(dir.getCanonicalPath(), dirname, testname, failsOnly, expect_failure));
                  } else if (s.endsWith(".sh")) {
                      int l = s.lastIndexOf(".sh");
                      String testname = s.substring(0, l);
                      suite.addTest(new ShellTest(dir.getCanonicalPath(), dirname, testname, failsOnly, expect_failure));
                  } else {
                      System.out.println("Not compiling file " + s);
                      decrement = false;
                  }
              }

              if (decrement) {
                  i--;
              }

        }
        return suite;
    }

    public static TestSuite compilerSuite(
            String dir_name,
            boolean failsOnly,
            boolean expect_failure) throws IOException {
        
        TestSuite suite = new TestSuite("Runs all tests in " + dir_name) {
            public void run(TestResult result) {
                super.run(result);
                Init.allowForLeakChecks();
            }
        };
        
        String dirname = ProjectProperties.backslashToSlash(dir_name);
        File dir = directoryAsFile(dirname);
        System.err.println(dir);
        
        /* Many lines of random number generator seed nonsense. */
        
        Iterable<String> shuffled = shuffledFileList(dir);

        int testCount = testCount();
        int i = testCount;
        
        List<Test> compileTests = new ArrayList<Test>();
        List<Test> linkTests = new ArrayList<Test>();
        List<Test> runTests = new ArrayList<Test>();
        
        for(String s : shuffled){
              if (i <= 0) {
                  System.out.println("Early testing exit after " + testCount + " tests");
                  break;
              }
              boolean decrement = true;
              if (s.endsWith(".fss") || s.endsWith(".fsi") ) {
                  // do nothing
                  decrement = false;
              } else if (!s.startsWith(".")) {
                  if (s.endsWith(".sh")) {
                      int l = s.lastIndexOf(".sh");
                      String testname = s.substring(0, l);
                      suite.addTest(new ShellTest(dir.getCanonicalPath(), dirname, testname, failsOnly, expect_failure));
                  } else if (s.endsWith(".test")) { // need to define the test of tests.
                      StringMap props = new StringMap.FromFileProps(dirname+"/"+s);
                      
                      int l = s.lastIndexOf(".test");
                      String testname = s.substring(0, l);
                      if (props.get("compile") != null)
                          compileTests.add(new CompileTest(dir.getCanonicalPath(), dirname, testname, failsOnly, expect_failure));
                      if (props.get("link") != null)
                          linkTests.add(new LinkTest(dir.getCanonicalPath(), dirname, testname, failsOnly, expect_failure));
                      if (props.get("run") != null)
                          runTests.add(new TestTest(props, dir.getCanonicalPath(), dirname, testname, failsOnly, expect_failure));
                  } else {
                      System.out.println("Not compiling file " + s);
                      decrement = false;
                  }
              }

              if (decrement) {
                  i--;
              }

        }
        // Do all the larger tests
        if (i > 0) {
            for (Test test: compileTests)
                suite.addTest(test);
            
            for (Test test: linkTests)
                suite.addTest(test);
            
            for (Test test: runTests)
                suite.addTest(test);
        }
        return suite;
    }

    /**
     * @param dirname
     * @return
     * @throws FileNotFoundException
     */
    private static File directoryAsFile(String dirname)
            throws FileNotFoundException {
        File dir = new File(dirname);
        if (!dir.exists()) {
            System.err.println(dirname + " does not exist");
            throw new FileNotFoundException(dirname);
        }
        if (!dir.isDirectory()) {
            System.err.println(dirname + " exists but is not a directory");
            throw new IllegalArgumentException(dirname);
        }
        return dir;
    }

    /**
     * @return
     */
    private static int testCount() {
        int testCount = Integer.MAX_VALUE;
        try {
            testCount = ProjectProperties.getInt("fortress.unittests.count",
                    Integer.MAX_VALUE);
        } catch (NumberFormatException ex) {
            System.err
                    .println("Failed to translate property fortress.unittests.count as an int, string = "
                            + ProjectProperties.get("fortress.unittests.count"));
            System.err.println("Expected a number in the form DIGITS (base 10) or DIGITS_BASE");
        }
        
        /* Limited testing count, such as it is. */
        if (testCount != Integer.MAX_VALUE)
            System.err.println("Test count = " + testCount);
        return testCount;
    }

    /**
     * @param files
     * @return
     */
    private static Iterable<String> shuffledFileList(File dir) {
        String[] files = dir.list();
        long default_seed = System.currentTimeMillis();
        long seed = default_seed;
        try {
            seed = ProjectProperties.getLong("fortress.unittests.seed",
                    default_seed);
        } catch (NumberFormatException ex) {
            System.err
                    .println("Failed to translate property fortress.unittests.seed as a long, string = "
                            + ProjectProperties.get("fortress.unittests.seed"));
            System.err.println("Expected a number in the form DIGITS (base 10) or DIGITS_BASE");
        }
        Random random = new java.util.Random(seed);
        Iterable<String> shuffled = IterUtil.shuffle(Arrays.asList(files),
                random);
        System.err.println("Test shuffling seed env var = FORTRESS_UNITTESTS_SEED="
                + Long.toHexString(seed) + "_16");
        return shuffled;
    }
}
