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

package com.sun.fortress.syntax_abstractions;

import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.compiler.StaticTestSuite;
import com.sun.fortress.interpreter.drivers.ProjectProperties;

public class SyntaxAbstractionJUTest extends TestCase {
    
    private final static String STATIC_TESTS_DIR = ProjectProperties.BASEDIR + "static_tests/syntax_abstraction/";
    private final static List<String> FAILING_SYNTAXABSTRACTIONS = Arrays.asList(
        "SyntaxASTUse.fss",
        "SyntaxAST.fss",
        "SyntaxGrammarImportsUse.fss",
        "SyntaxGrammarImports.fss",
        "SyntaxGrammarImportsA.fss",
        "SyntaxHiUse.fss",
        "SyntaxHi.fss",
        // really not working:
        // "XXXSyntaxNoFortressAstImport.fsi",
        "XXXSyntaxGrammarImportsUse.fss",
        "XXXSyntaxMultipleNonterminalDefsWithSameName.fsi");
    
    public static TestSuite suite() {
        return new StaticTestSuite("SyntaxAbstractionJUTest",
                                   STATIC_TESTS_DIR,
                                   FAILING_SYNTAXABSTRACTIONS,
                                   null);     
    }
    
}
