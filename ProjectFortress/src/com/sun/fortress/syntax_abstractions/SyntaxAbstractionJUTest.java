/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions;

import com.sun.fortress.Shell;
import com.sun.fortress.compiler.StaticTestSuite;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.repository.ProjectProperties;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;

public class SyntaxAbstractionJUTest extends TestSuite {

    private static final char SEP = File.separatorChar;
    private final static String STATIC_TESTS_DIR = ProjectProperties.BASEDIR + "syntax_abstraction_tests" + SEP;

    public static TestSuite suite() throws IOException {
        String[] files = new String[]{
                // List trimmed to keep testing time quick.
                "ForUse.fss"
        };

        TestSuite suite = new TestSuite("SyntaxAbstractionJUTest");
        /*
        GraphRepository fr =
                Shell.specificInterpreterRepository(ProjectProperties.SOURCE_PATH.prepend(STATIC_TESTS_DIR));
        */
        for (String filename : files) {
            File f = new File(STATIC_TESTS_DIR + filename);
            suite.addTest(new StaticTestSuite.StaticTestCase(f, false));
        }
        return suite;
    }
}
