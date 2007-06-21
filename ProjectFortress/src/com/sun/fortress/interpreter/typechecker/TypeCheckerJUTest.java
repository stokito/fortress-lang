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

import java.io.File;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.interpreter.drivers.Driver;
import com.sun.fortress.interpreter.evaluator.Init;
import com.sun.fortress.interpreter.nodes.CompilationUnit;
import com.sun.fortress.interpreter.useful.Useful;
import com.sun.fortress.interpreter.useful.TcWrapper;

/**
 * Tests that specific files in the tests directory contain static errors.  Ensuring that
 * all files in "tests" that don't start with "XXX" pass type checking is done by the 
 * drivers/SystemJUTests test.  All "XXX" files not listed below are expected to only have runtime errors.
 */
public class TypeCheckerJUTest extends TcWrapper {
    
    private void checkErrorCount(String filePrefix, int expectedCount) throws IOException {
        String fileName = "type_errors" + File.separator + filePrefix + ".fss";
        CompilationUnit c = Driver.parseToJavaAst(fileName);
        TypeCheckerResult result = TypeChecker.check(c);
        
        assertEquals("Incorrect number of static errors reported for " + filePrefix + ".fss.", expectedCount, result.errorCount());
    }
    
    public void testUndefinedVar() throws IOException { checkErrorCount("UndefinedVar", 1); }
    public void testUndefinedArrayRef() throws IOException { checkErrorCount("UndefinedArrayRef", 1); }
    public void testUndefinedNestedRef() throws IOException { checkErrorCount("UndefinedNestedRef", 1); }
    public void testUndefinedRefInLoop() throws IOException { checkErrorCount("UndefinedRefInLoop", 1); }
    public void testMultipleRefErrors() throws IOException { checkErrorCount("MultipleRefErrors", 4); }
    
}
