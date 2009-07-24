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
