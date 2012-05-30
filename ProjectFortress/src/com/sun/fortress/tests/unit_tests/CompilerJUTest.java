/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.tests.unit_tests;

import com.sun.fortress.Shell;
import com.sun.fortress.repository.ProjectProperties;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;

public class CompilerJUTest {
    public static void main(String[] args) throws IOException {
        // Make sure there are no compiled files from earlier tests
        // lurking in the repository.
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() throws IOException {
        // Make sure there are no compiled files from earlier tests
        // lurking in the repository.
        Shell.resetRepository();
        
        String testDir1 = ProjectProperties.BASEDIR + "compiler_tests";
        String testDir2 = ProjectProperties.BASEDIR + "parser_tests";
        boolean failsOnly = !ProjectProperties.getBoolean("fortress.junit.verbose", false);
        TestSuite suite = new TestSuite(
                "Test all .test files in 'ProjectFortress/compiler_tests' " + "and 'ProjectFortress/parser_tests'.");
        //$JUnit-BEGIN$
        suite.addTest(FileTests.compilerSuite(testDir1, true, failsOnly, false));
        suite.addTest(FileTests.compilerSuite(testDir2, true, failsOnly, false));
        return suite;
    }

}
