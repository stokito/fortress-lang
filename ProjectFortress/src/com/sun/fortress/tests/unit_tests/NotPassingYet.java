/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.tests.unit_tests;

import com.sun.fortress.repository.ProjectProperties;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;

public class NotPassingYet {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(NotPassingYet.class);
    }

    public static Test suite() throws IOException {
        String testDir = ProjectProperties.BASEDIR + "not_passing_yet";
        TestSuite suite = new TestSuite("Test all .fss files in 'tests'.");
        //$JUnit-BEGIN$
        suite.addTest(FileTests.interpreterSuite(testDir, true, true));
        //$JUnit-END$
        return suite;
    }

}
