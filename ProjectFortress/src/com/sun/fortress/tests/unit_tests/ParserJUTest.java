/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.tests.unit_tests;

import com.sun.fortress.compiler.Parser;
import static com.sun.fortress.exceptions.ProgramError.error;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.Files;
import com.sun.fortress.useful.TestCaseWrapper;
import com.sun.fortress.useful.WireTappedPrintStream;
import edu.rice.cs.plt.iter.IterUtil;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

public class ParserJUTest extends TestCaseWrapper {

    private static final String SEP = File.separator;
    private final static String PARSER_FAIL_TESTS_DIR = ProjectProperties.BASEDIR + "parser_tests" + SEP;
    private final static String PARSER_NYI_TESTS_DIR = ProjectProperties.BASEDIR + "not_passing_yet" + SEP;

    private static boolean isNYI(String parent) {
        return parent.contains("not_passing_yet");
    }

    public static TestSuite suite() {
        return new ParserTestSuite("ParserJUTest", PARSER_FAIL_TESTS_DIR, PARSER_NYI_TESTS_DIR);
    }

    private final static class ParserTestSuite extends TestSuite {
        private final static boolean VERBOSE = false;

        // relative to the top ProjectFortress directory
        private final String failTestDir;
        private final String nyiTestDir;

        public ParserTestSuite(String _name, String _failTestDir, String _nyiTestDir) {
            super(_name);
            failTestDir = _failTestDir;
            nyiTestDir = _nyiTestDir;
            addParserTests();
        }

        private void addParserTests() {
            FilenameFilter fssFilter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (name.endsWith("fss") || name.endsWith("fsi"));
                }
            };

            //Permute filenames for randomness
            String[] files = new File(failTestDir).list(fssFilter);
            Iterable<String> shuffled = IterUtil.shuffle(Arrays.asList(files));
            for (String filename : shuffled) {
                File f = new File(failTestDir + filename);
                addTest(new ParserTestCase(f));
            }
            //Permute filenames for randomness
            files = new File(nyiTestDir).list(fssFilter);
            shuffled = IterUtil.shuffle(Arrays.asList(files));
            for (String filename : shuffled) {
                File f = new File(nyiTestDir + filename);
                addTest(new ParserTestCase(f));
            }
        }

        private static class ParserTestCase extends TestCase {

            private final File file;

            /**
             * Construct a parser test case for the given file.
             */
            public ParserTestCase(File _file) {
                super(_file.getName());
                file = _file;
            }

            @Override
            protected void runTest() throws Throwable {
                String name = file.getName();
                String parent = file.getParent();

                // do not print stuff to stdout for JUTests
                PrintStream oldOut = System.out;
                PrintStream oldErr = System.err;
                WireTappedPrintStream wt_err = WireTappedPrintStream.make(System.err, true);
                WireTappedPrintStream wt_out = WireTappedPrintStream.make(System.out, true);
                System.setErr(wt_err);
                System.setOut(wt_out);

                if (!isNYI(parent)) {
                    if (name.contains("XXX")) assertParserFails(file);
                    else assertParserSucceeds(file);
                } else if (isNYI(parent)) {
                    assertParserSucceeds(file);
                } else {
                    error("Unexpected file in the parser_test directory: " + name);
                }
                System.setErr(oldErr);
                System.setOut(oldOut);
            }

            private void assertParserFails(File f) throws IOException {
                try {
                    Parser.Result result = parseFile(f);
                    assertFalse("Source " + f + " was compiled without parser errors", result.isSuccessful());
                }
                catch (Throwable e) {
                }
            }

            private void assertParserSucceeds(File f) throws IOException {
                Parser.Result result = parseFile(f);
                assertFalse("Source " + f + " was compiled with parser errors", !result.isSuccessful());
            }

            private Parser.Result parseFile(File f) {
                try {
                    return new Parser.Result(Parser.parseFileConvertExn(f), f.lastModified());
                }
                catch (StaticError se) {
                    return new Parser.Result(se);
                }
                finally {
                    try {
                        Files.rm(ProjectProperties.preparserErrorLog(f));
                    }
                    catch (IOException e) {
                    }
                }
            }
        }
    }
}
