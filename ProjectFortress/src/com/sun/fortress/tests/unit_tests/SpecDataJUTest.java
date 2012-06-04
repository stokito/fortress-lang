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
import junit.framework.TestSuite;

import java.io.IOException;

public class SpecDataJUTest {
    public static void main(String[] args) throws IOException {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() throws IOException {
    	Shell.setCompiledExprDesugaring(false);
        Shell.setPhaseOrder(PhaseOrder.interpreterPhaseOrder);
    	
        String testDir1 = ProjectProperties.FORTRESS_AUTOHOME + "/SpecData/examples/basic";
        String testDir2 = ProjectProperties.FORTRESS_AUTOHOME + "/SpecData/examples/preliminaries";
        String testDir3 = ProjectProperties.FORTRESS_AUTOHOME + "/SpecData/examples/advanced";
        boolean failsOnly = !("1".equals(ProjectProperties.get("FORTRESS_JUNIT_VERBOSE")));
        TestSuite suite = new TestSuite("Test all .fss files in 'SpecData/examples'.");
        //$JUnit-BEGIN$
        suite.addTest(FileTests.interpreterSuite(testDir1, failsOnly, false));
        suite.addTest(FileTests.interpreterSuite(testDir2, failsOnly, false));
        suite.addTest(FileTests.interpreterSuite(testDir3, failsOnly, false));
        //$JUnit-END$
        return suite;
    }

}
