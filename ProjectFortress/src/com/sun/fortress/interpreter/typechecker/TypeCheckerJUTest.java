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

public class TypeCheckerJUTest extends TestCase {

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        Init.initializeEverything();
    }

    private static final String[] valid = {
        "varTest"
    };
    
    private static final String[] invalid = {
        "XXXvarTest"
    };
    
    public void testValid() throws IOException {
        for (String name : valid) {
            String f = "tests" + File.separator + name + ".fss";
            CompilationUnit c = Driver.parseToJavaAst(f, Useful.utf8BufferedFileReader(f));
            TypeChecker.check(c);
        }
    }
}
