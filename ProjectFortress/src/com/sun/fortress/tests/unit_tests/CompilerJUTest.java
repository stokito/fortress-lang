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
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;
import com.sun.fortress.repository.ProjectProperties;

public class CompilerJUTest {


    public static void main(String[] args) throws IOException {

        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() throws IOException {
        String testDir = ProjectProperties.BASEDIR + "compiler_tests";
        boolean failsOnly = ! ("1".equals(System.getenv("FORTRESS_JUNIT_VERBOSE")));
        
       // TestSuite suite = new TestSuite("Test all .fss files in 'tests'.");
        //$JUnit-BEGIN$
       // suite.addTest(FileTests.suite(testDir, failsOnly, false));
        //$JUnit-END$
        return FileTests.compilerSuite(testDir, failsOnly, false);
    }

}
