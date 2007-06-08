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

package com.sun.fortress.interpreter.typechecker;
import junit.framework.TestCase;
import java.io.File;
import java.io.IOException;

import com.sun.fortress.interpreter.drivers.Driver;
import com.sun.fortress.interpreter.evaluator.Init;
import com.sun.fortress.interpreter.nodes.CompilationUnit;
import com.sun.fortress.interpreter.useful.Useful;

/**
 * Tests that specific files in the tests directory contain static errors.  Ensuring that
 * all files in "tests" that don't start with "XXX" pass type checking is done by the 
 * drivers/SystemJUTests test.  All "XXX" files not listed below are expected to only have runtime errors.
 */
public class TypeCheckerJUTest extends TestCase {

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        Init.initializeEverything();
    }

    private static final String[] typeErrors = {
        "UndefinedVar",
        "EmptyBlock",
        "UndefinedArrayRef",
        "UndefinedNestedRef"
    };
    
    public void testTypeErrors() throws IOException {
        for (String name : typeErrors) {
            String f = "type_errors" + File.separator + name + ".fss";
            CompilationUnit c = Driver.parseToJavaAst(f, Useful.utf8BufferedFileReader(f));
            try {
                TypeChecker.check(c);
                fail("Checked " + f + " without a type error");
            }
            catch (TypeError e) {
                System.err.println(f + " OK");
            }
        }
    }
}
