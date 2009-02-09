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

package com.sun.fortress.syntax_abstractions;

import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.tests.unit_tests.FileTests;

public class SyntaxAbstractionJUTest {

    private final static String STATIC_TESTS_DIR =
        ProjectProperties.BASEDIR + "static_tests/syntax_abstraction";

    public static void main(String[] args) throws IOException {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() throws IOException {
        String[] files = new String[]{
            // List trimmed to keep testing time quick.
            "CaseUse.fss",
            "DoubleCaseUse.fss",
            "GrammarCompositionUseA.fss",
            "GrammarCompositionUseB.fss",
            "GrammarCompositionUseC.fss",
            "GrammarCompositionUseD.fss",
            "ForUse.fss",
            "OrUse.fss",
            "LabelUse.fss",
            "SyntaxNodesUse.fss",
        };

        TestSuite suite = new TestSuite("SyntaxAbstractionJUTest");
        for ( String filename : files ){
            String testname = filename.substring(0, filename.lastIndexOf(".fss"));
            suite.addTest(new FileTests.FSSTest(STATIC_TESTS_DIR, STATIC_TESTS_DIR,
                                                testname, true, false));
        }
        return suite;
    }
}
