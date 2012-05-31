/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.tests.unit_tests;

import com.sun.fortress.Shell;
import junit.framework.Test;

import java.io.IOException;

/**
 * This class functions just like CompilerJUTest, but it invalidates
 * the cache before running. This is necessary in order to allow compiler
 * tests and interpreter tests to be run in the same Ant target.
 */
public class NightlyCompilerJUTest {
    public static void main(String[] args) throws IOException {
        // Make sure there are no compiled files from earlier tests
        // lurking in the repository.
        Shell.resetRepository();
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() throws IOException {
        return CompilerJUTest.suite();
    }

}
