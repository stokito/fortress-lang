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

import java.io.File;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.Shell;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.compiler.StaticTestSuite;
import com.sun.fortress.repository.ProjectProperties;

public class SyntaxAbstractionJUTest extends TestSuite {

    private static final char SEP = File.separatorChar;
    private final static String STATIC_TESTS_DIR =
        ProjectProperties.BASEDIR + "syntax_abstraction_tests" + SEP;

    public static TestSuite suite() throws IOException {
        String[] files = new String[]{
            // List trimmed to keep testing time quick.
            "ForUse.fss"
        };

        TestSuite suite = new TestSuite("SyntaxAbstractionJUTest");
        GraphRepository fr = Shell.specificInterpreterRepository( ProjectProperties.SOURCE_PATH.prepend(STATIC_TESTS_DIR) );
        for ( String filename : files ){
            File f = new File(STATIC_TESTS_DIR + filename );
            suite.addTest(new StaticTestSuite.StaticTestCase(f, false));
        }
        return suite;
    }
}
