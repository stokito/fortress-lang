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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.interpreter.nodes.CompilationUnit;
import com.sun.fortress.interpreter.nodes.Unprinter;
import com.sun.fortress.interpreter.reader.Lex;
import com.sun.fortress.interpreter.useful.Useful;
import com.sun.fortress.interpreter.useful.WireTappedPrintStream;

public class FileTests {


    public static class FSSTest extends TestCase {
        String f;
        boolean failsOnly;

        public FSSTest(String d, String s, boolean failsOnly) {
            super("testFile");
            this.f = d + "/" + s;
            this.failsOnly = failsOnly;

        }

        public String getName() {
            return f;
        }

        public void testFile() throws Throwable {
            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;
            WireTappedPrintStream wt_err =
                WireTappedPrintStream.make(System.err, failsOnly);
            WireTappedPrintStream wt_out =
                WireTappedPrintStream.make(System.out, failsOnly);
            System.setErr(wt_err);
            System.setOut(wt_out);

            try {
                try {
                    oldOut.print(f); oldOut.print(" "); oldOut.flush();
                    String s = f.replaceFirst(".*/", "");
                    String tmpFile = "/tmp/" + s + ".ast";
                    String fssFile = f + ".fss";
                    Annotations anns = new Annotations(fssFile);
                    CompilationUnit p;
                    p = Driver.parseToJavaAst(fssFile, Useful.utf8BufferedFileReader(fssFile));

                    if (anns.compile) {
                        // oldOut.print(" COMPILING"); oldOut.flush();
                        Driver.evalComponent(p);
                    }
                    else {
                        // oldOut.print(" RUNNING"); oldOut.flush();
                        if (!failsOnly) System.out.println();
                        Driver.runProgram(p, true, new ArrayList<String>());
                    }

                } finally {
                    System.setErr(oldErr);
                    System.setOut(oldOut);
                }
            } catch (Throwable ex) {
                if (f.contains("XXX")) {
                    wt_err.flush(false);
                    wt_out.flush(false);
                    String exFirstLine = ex.toString();
                    int crLoc = exFirstLine.indexOf("\n");
                    if (crLoc == -1) crLoc = exFirstLine.length();
                    exFirstLine = exFirstLine.substring(0, crLoc);
                    System.out.println(" OK Saw expected exception " + exFirstLine );
                    return;
                } else {
                    // Unexpected
                    if (failsOnly) System.out.println();
                    wt_err.flush(true);
                    wt_out.flush(true);
                    System.out.println(" UNEXPECTED exception " + ex);
                    throw ex;
                }
            }

            /* Come here IFF NO EXCEPTIONS, to analyze output */

            String outs = wt_out.getString();
            // String errs = wt_err.getString();

            if (f.contains("XXX")) {
                // NOTE expect to see this on STANDARD OUTPUT, not ERROR.
                if (outs.contains("fail") || outs.contains("FAIL")) {
                    wt_err.flush(false);
                    wt_out.flush(false);
                    // Saw a failure, that is good.
                    System.out.println(" Saw expected failure " );
                } else {
                    if (failsOnly) System.out.println();
                    wt_err.flush(true);
                    wt_out.flush(true);
                    // Expected exception, saw none.
                    fail("Expected failure or exception, saw none.");
                }
            } else {
                boolean anyFails = outs.contains("fail") || outs.contains("FAIL");
                    System.out.println(anyFails ? " FAIL" : " OK");
                    wt_err.flush(anyFails);
                    wt_out.flush(anyFails);

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
            BufferedReader br = new BufferedReader(new FileReader(d + "/" + s + ".jst"));
            Lex lex = new Lex(br);
            Unprinter up = new Unprinter(lex);
            lex.name(); // Inhale opening parentheses
            CompilationUnit p = (CompilationUnit) up.readNode(lex.name());
            Driver.runProgram(p, true, new ArrayList<String>());
        }

    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FileTests.suite("tests", true));
        junit.textui.TestRunner.run(FileTests.suite("not_passing_yet", false));
    }

    public static Test suite(String dirname, boolean failsOnly) {
        TestSuite suite = new TestSuite("Test for default package");
        // $JUnit-BEGIN$
        dirname = ProjectProperties.backslashToSlash(dirname);
        File dir = new File(dirname);
        String[] files = dir.list();
        System.err.println(dir);
        for (int i = 0; i < files.length - 1; i++) {
            String s = files[i];
            if (!s.startsWith(".")) {
                if (s.endsWith(".fss")) {
                    int l = s.lastIndexOf(".fss");
                    System.err.println("Adding " + s);
                    suite.addTest(new FSSTest(dirname, s.substring(0, l), failsOnly));
                } else if (s.endsWith(".jst")) {
                    int l = s.lastIndexOf(".jst");
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
