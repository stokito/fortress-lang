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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.compiler.StaticTestSuite;
import com.sun.fortress.compiler.StaticTestSuite.TestCaseDir;
import com.sun.fortress.repository.ProjectProperties;

public class SyntaxAbstractionJUTestAll extends TestCase {
    
    private final static String STATIC_TESTS_DIR = ProjectProperties.BASEDIR + "static_tests/syntax_abstraction/";
    private final static List<String> FAILING_SYNTAXABSTRACTIONS = Arrays.asList(
        "SyntaxASTUse.fss",
        "SyntaxAST.fss",
        "ChurchBooleansUse.fss",
        "ChurchBooleans.fss",
        "ChurchBooleans.fsi",
        "XXXSyntaxGrammarImportsUse.fss",
        "ImportApiEmptyApiWhichImportsNonEmptyApiUse.fss",
        "UsingJavaIdentifiersAsPatternVariablesUse.fss");
        
//        "SyntaxTest.fss",
//        "SyntaxGrammarImportsUse.fss",
//        "SyntaxRepetition.fss",
//        "SyntaxOptionUse.fss",
//        "SyntaxHelloWorldUse.fss",
//        "SyntaxTestUse.fss",
//        "SyntaxRepetitionUse.fss",
//        "SyntaxGrammarImports.fss",
//        "SyntaxGrammarImportsA.fss",
//        "SyntaxGrammarImportsUse.fss",
//        "SXXSyntaxMultipleNonterminalDefsWithSameName.fss",
//        "SyntaxSymbolsUse.fss",
//        "SyntaxOption.fss",
//        "SyntaxHelloWorld.fss",
//        "SyntaxExtendsUse.fss",
//        "SyntaxExtends.fss",
//        "SyntaxHelloWorldTemplateUse.fss",
//        "SyntaxHelloWorldTemplate.fss",
//        "SXXSyntaxMultipleNonterminalDefsWithSameNameUse.fss",
//        "SyntaxNodes.fss",
//        "SyntaxNodesUse.fss",
//        "SyntaxTemplateVars.fss",
//        "SyntaxTemplateVarsUse.fss",
//        "SyntaxSymbols.fss",
//        "SXXGrammarExtendsNonExistingGrammarUse.fss",
//        "SXXMultipleGrammarsWithSameNameUse.fss",
//        "Comprehension.fss",
//        "ComprehensionUse.fss",
//        "TemplateGapWithInconsistentParameters.fss",
//        "SXXMultipleGrammarsWithSameName.fss",
//        "ImportEmptyApiWhichImportsNonEmptyApiEmpty.fss",
//        "ImportEmptyApiWhichImportsNonEmptyApiUse.fss",
//        "TemplateGapWithWrongASTType.fss",
//        "SXXTemplateGapWithInconsistentParametersUse.fss",
//        "ImportEmptyApiWhichImportsNonEmptyApiNonEmpty.fss");
    
    public static TestSuite suite() {
        TestCaseDir xml = new TestCaseDir(STATIC_TESTS_DIR+"/xml/", Collections.<String>emptyList(), null);
        TestCaseDir regex = new TestCaseDir(STATIC_TESTS_DIR+"/regex/", Collections.<String>emptyList(), null);
        TestCaseDir sql = new TestCaseDir(STATIC_TESTS_DIR+"/sql/", Collections.<String>emptyList(), null);
        TestCaseDir basisTests = new TestCaseDir(STATIC_TESTS_DIR, FAILING_SYNTAXABSTRACTIONS, null);
        
        StaticTestSuite suite = new StaticTestSuite("SyntaxAbstractionJUTestAll", basisTests);
        suite.addStaticTests(xml);
        // suite.addStaticTests(regex);
        // suite.addStaticTests(sql);
        return suite;
    }
    
}
