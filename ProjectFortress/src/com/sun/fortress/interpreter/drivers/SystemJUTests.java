/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.drivers;
import junit.framework.Test;
import junit.framework.TestSuite;

public class SystemJUTests {


    public static void main(String[] args) {

        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        String testDir = ProjectProperties.BASEDIR + "tests";
        String s = System.getProperty("tests");
        if (s != null) {
            testDir = s;
        }
        TestSuite suite = new TestSuite("Test all .fss files in 'tests'.");
        //$JUnit-BEGIN$
        suite.addTest(FileTests.suite(testDir, true, false));
        //$JUnit-END$
        return suite;
    }

}
