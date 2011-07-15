/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;

import com.sun.fortress.Shell;
import com.sun.fortress.compiler.phases.PhaseOrder;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.TypeError;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.exceptions.shell.UserError;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.WireTappedPrintStream;

public final class StaticTestSuite extends TestSuite {

    private final static boolean VERBOSE = false;
    private final static boolean SKIP_FAILING = true;

    public StaticTestSuite(String _name, StaticTestSuite.TestCaseDir testCaseDir) {
        super(_name);
        addStaticTests(testCaseDir);
    }

    public StaticTestSuite(String _name, String _testDir, List<String> _failingDisambiguator, List<String> _failingTypeChecker) {
        super(_name);
        StaticTestSuite.TestCaseDir testCaseDir = new StaticTestSuite.TestCaseDir(_testDir, _failingDisambiguator, _failingTypeChecker);
        addStaticTests(testCaseDir);
    }

    public void addStaticTests(TestCaseDir testCase) {
        FilenameFilter fssFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith("fss");
            }
        };

        //Permute filenames for randomness
        String[] files = new File(testCase.getTestDir()).list(fssFilter);
        Iterable<String> shuffled = IterUtil.shuffle(Arrays.asList(files));
        for (String filename : shuffled) {
            File f = new File(testCase.getTestDir() + filename);

            if (SKIP_FAILING && testCase.isFailingDisambiguator(f)) {
                // skip
            } else if (testCase.getSkipTypeChecker() || (SKIP_FAILING && testCase.isFailingTypeChecker(f))) {
                addTest(new StaticTestCase(f, false));
            } else {
                addTest(new StaticTestCase(f));
            }
        }
    }

    public static class TestCaseDir {
        // relative to the top ProjectFortress directory
        private String testDir;
        private boolean skipTypeChecker;
        private Set<File> failingDisambiguator;
        private Set<File> failingTypeChecker;

        public TestCaseDir(String _testDir, List<String> _failingDisambiguator, List<String> _failingTypeChecker) {
            testDir = _testDir;
            skipTypeChecker = (_failingTypeChecker == null);
            failingDisambiguator = fileSetFromStringList(_failingDisambiguator);
            failingTypeChecker = fileSetFromStringList(_failingTypeChecker);
        }

        public String getTestDir() {
            return this.testDir;
        }

        public boolean getSkipTypeChecker() {
            return this.skipTypeChecker;
        }

        protected boolean isFailingDisambiguator(File f) {
            return this.failingDisambiguator.contains(f);
        }

        protected boolean isFailingTypeChecker(File f) {
            return this.failingDisambiguator.contains(f) || this.failingTypeChecker.contains(f);
        }



        private Set<File> fileSetFromStringList(List<String> files) {
            if (files == null) {
                return CollectUtil.<File>emptySet();
            } else {
                return CollectUtil.asSet(IterUtil.map(files, new Lambda<String, File>() {
                    public File value(String s) { return new File(testDir + s); }
                }));
            }
        }

    }

    public static class StaticTestCase extends TestCase {

        private final File file;
        private final boolean typecheck;
        private boolean typecheckStore = true;

        /**
         * Construct a static test case for the given file.  The file will be type checked
         * as dictated by the file name.
         */
        public StaticTestCase(File _file) {
            super(_file.getName());
            file = _file;
            typecheck = true;
        }

        /**
         * Construct a static test case for the given file.  The typecheck parameter should
         * be false if the test should not be type checked (i.e. if the file name dictates
         * that it should be, but it is not currently passing type checking).
         */
        public StaticTestCase(File _file, boolean _typecheck) {
            super(_file.getName() + (_typecheck ? "" : " (type checking disabled)"));
            file = _file;
            typecheck = _typecheck;
        }

        @Override
        protected void setUp() throws Exception {
            typecheckStore = Shell.getTypeChecking();
            Shell.setTypeChecking(typecheck);
        }

        @Override
        protected void tearDown() throws Exception {
            Shell.setTypeChecking(typecheckStore);
        }

        @Override
        protected void runTest() throws Throwable {
            if (file.getName().startsWith("XXX")) {
                assertDisambiguatorFails(file);
            } else if (file.getName().startsWith("SXX")) {
                assertSyntaxAbstractionFails(file);
            } else if (file.getName().startsWith("DXX") && typecheck) {
                assertTypeCheckerFails(file);
            } else {
                assertWellFormedProgram(file);
            }
        }

        private void assertDisambiguatorFails(File f) throws IOException, UserError {
            Shell.setTypeChecking(false);
            Iterable<? extends StaticError> errors = compile(f);
            assertFalse("Source " + f + " was compiled without disambiguator errors",
                        IterUtil.isEmpty(errors));
            if (VERBOSE) {
                System.out.println(f + "  OK -- errors:");
                System.out.println(IterUtil.multilineToString(errors));
            }
        }

        private void assertSyntaxAbstractionFails(File f) throws IOException, UserError {
            Shell.setTypeChecking(false);
            try{

                PrintStream oldOut = System.out;
                PrintStream oldErr = System.err;
                WireTappedPrintStream wt_err =
                    WireTappedPrintStream.make(System.err, true);
                WireTappedPrintStream wt_out =
                    WireTappedPrintStream.make(System.out, true);
                System.setErr(wt_err);
                System.setOut(wt_out);
                Iterable<? extends StaticError> errors = compile(f);
                System.setErr(oldErr);
                System.setOut(oldOut);
                System.out.println("  " + f + "  OK");

                assertFalse("Source " + f + " was compiled without syntax abstraction errors",
                            IterUtil.isEmpty(errors));

            } catch ( MacroError error ){
                if (VERBOSE) {
                    System.out.println(f + "  OK -- errors:");
                    error.printStackTrace();
                }
            }
        }

        private List<TypeError> findTypeErrors( StaticError error ){
            try{
                throw error;
            } catch ( TypeError t ){
                return Collections.singletonList(t);
            } catch ( WrappedException w ){
                try{
                    throw w.getCause();
                } catch ( StaticError s ){
                    return findTypeErrors(s);
                } catch ( Throwable t ){
                    t.printStackTrace();
                    fail("Saw a non-static error");
                    /* ignore it */
                }
            } catch ( MultipleStaticError errors ){
                List<TypeError> all = new ArrayList<TypeError>();
                for ( StaticError s : errors ){
                    all.addAll(findTypeErrors(s));
                }
                return all;
            }
            return Collections.emptyList();
        }

        private void assertTypeCheckerFails(File f) throws IOException, UserError {
            List<TypeError> typeErrors = new ArrayList<TypeError>();
            String message = "";
            Iterable<? extends StaticError> allErrors = compile(f);
            if (!IterUtil.isEmpty(allErrors)) {
                message = " but got:";
                for (StaticError error : allErrors) {
                    typeErrors.addAll(findTypeErrors(error));
                    /*
                    try { throw error; }
                    catch (TypeError e) { typeErrors.add(e); }
                    catch ( MultipleStaticError errors ){
                        for ( StaticError e : errors ){
                            if ( e instanceof TypeError ){
                                typeErrors.add((TypeError) e);
                            }
                        }
                    } catch (WrappedException e) {
                        try{
                            throw e.getCause();
                        } catch ( TypeError e1 ){
                            typeErrors.add(e1);
                        } catch ( Throwable e1 ){
                            System.out.println( e1.getClass().getName() );
                            e1.printStackTrace();
                            message += "\nStaticError (wrapped): " + e1.toString();
                        }
                    }
                    catch (StaticError e) { message += "\nStaticError: " + e.toString(); }
                    */
                }
            }

            /*
            for (StaticError error : allErrors) {
                try { throw error; }
                catch (TypeError e) { typeErrors.add(e); }
                catch ( MultipleStaticError errors ){
                    for ( StaticError e : errors ){
                        if ( e instanceof TypeError ){
                            typeErrors.add((TypeError) e);
                        }
                    }
                } catch (WrappedException e) {
                    try{
                        throw e.getCause();
                    } catch ( TypeError e1 ){
                        typeErrors.add(e1);
                    } catch ( Throwable e1 ){
                        e1.printStackTrace();
                        message += "\nStaticError (wrapped): " + e1.toString();
                    }
                }
                catch (StaticError e) { message += "\nStaticError: " + e.toString(); }
            }
            */
            assertFalse("Source " + f + " was compiled without TypeErrors" + message,
                        IterUtil.isEmpty(typeErrors));
            if (VERBOSE) {
                System.out.println(f + "  OK -- errors:");
                System.out.println(IterUtil.multilineToString(typeErrors));
            }
        }

        private void assertWellFormedProgram(File f)
            throws IOException, UserError {
            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;
            WireTappedPrintStream wt_err =
                WireTappedPrintStream.make(System.err, true);
            WireTappedPrintStream wt_out =
                WireTappedPrintStream.make(System.out, true);
            System.setErr(wt_err);
            System.setOut(wt_out);
            Iterable<? extends StaticError> errors = compile(f);
            System.setErr(oldErr);
            System.setOut(oldOut);

            String message = "Source " + f + " produces static errors:";
            StringBuilder buf = new StringBuilder();
            buf.append(message);
            for (StaticError error : errors) {
                try { throw error; }
                catch (WrappedException e) {
                    e.getCause().printStackTrace();
                    buf.append("\nStaticError (wrapped): " + e.getCause().toString());
                }
                catch (StaticError e) { buf.append("\nStaticError: " + e.toString()); }
            }
            message = buf.toString();
            assertTrue(message, IterUtil.isEmpty(errors));
            if (VERBOSE) { System.out.println(f + "  OK"); }
        }

        private Iterable<? extends StaticError> compile(File f)
                throws IOException, UserError {
            Shell.setPhaseOrder(PhaseOrder.interpreterPhaseOrder);
            return Shell.compilerPhases(ProjectProperties.SOURCE_PATH.prepend(f.getParent()),
                                        f.getName());
        }
    }
}
