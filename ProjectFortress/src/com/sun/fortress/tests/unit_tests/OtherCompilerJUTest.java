/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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
