/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.tests.unit_tests;

import com.sun.fortress.Shell;
import com.sun.fortress.compiler.phases.PhaseOrder;
import com.sun.fortress.repository.ProjectProperties;
import junit.framework.Test;

import java.io.IOException;

public class SystemJUTest {


    public static void main(String[] args) throws IOException {

        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() throws IOException {
        Shell.setPhaseOrder(PhaseOrder.interpreterPhaseOrder);
        Shell.setCompiledExprDesugaring(false);
        String testDir = ProjectProperties.BASEDIR + "tests";
        String s = System.getProperty("tests");
        boolean failsOnly = !("1".equals(System.getenv("FORTRESS_JUNIT_VERBOSE")));
        if (s != null) {
            testDir = s;
        }
        // TestSuite suite = new TestSuite("Test all .fss files in 'tests'.");
        //$JUnit-BEGIN$
        // suite.addTest(FileTests.suite(testDir, failsOnly, false));
        //$JUnit-END$
        return FileTests.interpreterSuite(testDir, failsOnly, false);
    }

}
