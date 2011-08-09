/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.tests.unit_tests;

import com.sun.fortress.repository.ProjectProperties;
import junit.framework.Test;

import java.io.IOException;


// This is for tests that don't fit in the CompiledX.fss group.

public class OtherCompilerJUTest {


    public static void main(String[] args) throws IOException {

        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() throws IOException {
        String testDir = ProjectProperties.BASEDIR + "other_compiler_tests";

        boolean failsOnly = !ProjectProperties.getBoolean("fortress.junit.verbose", false);

        return FileTests.compilerSuite(testDir, false, failsOnly, false);
    }

}
