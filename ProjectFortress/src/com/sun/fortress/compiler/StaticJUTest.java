/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler;

import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.TestCaseWrapper;

public class StaticJUTest extends TestCaseWrapper {

    private final static String STATIC_TESTS_DIR = ProjectProperties.BASEDIR + "static_tests/";
    private final static List<String> FAILING_DISAMBIGUATOR = Arrays.asList();
    private final static List<String> FAILING_TYPE_CHECKER = Arrays.asList();
    public static TestSuite suite() {
        return new StaticTestSuite("StaticJUTest",
                                   STATIC_TESTS_DIR,
                                   FAILING_DISAMBIGUATOR,
                                   FAILING_TYPE_CHECKER);
    }
}
