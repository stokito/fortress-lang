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
import java.util.List;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.compiler.StaticTestSuite;
import com.sun.fortress.compiler.StaticTestSuite.TestCaseDir;
import com.sun.fortress.repository.ProjectProperties;

public class SyntaxAbstractionJUTestAll extends TestCase {

    private final static String STATIC_TESTS_DIR = ProjectProperties.BASEDIR + "static_tests/syntax_abstraction/";
    private final static List<String> FAILING_SYNTAXABSTRACTIONS = Arrays.asList(
        "SyntaxAST.fss",
        "SyntaxASTUse.fss",
        "SyntaxRepetition.fss",
        "SyntaxRepetitionUse.fss",
        "SyntaxGrammarImportsUse.fss",
        "ImportApiEmptyApiWhichImportsNonEmptyApiUse.fss",
        "UsingJavaIdentifiersAsPatternVariables.fss",
        "UsingJavaIdentifiersAsPatternVariablesUse.fss",
        "XXXSyntaxGrammarImportsUse.fss");

//        "CatchUse.fss",
//        "GeneratorClauseUse.fss",
//        "TypecaseUse.fss",
//        "SXXKeywordNotIdUse.fss",
//        "SXXSyntaxGrammarImportsUse.fss",
//        "SXXTemplateParamsAreNotApplicableUse.fss",
//        "RegexUse1.fss",
//        "RegexUse2.fss",
//        "SqlUse.fss",
//        "XmlUse.fss",
//        "SXXMultipleGrammarsWithSameName.fss",
//        "SXXMultipleGrammarsWithSameNameUse.fss",
//        "SXXSyntaxMultipleNonterminalDefsWithSameName.fss",
//        "SXXSyntaxMultipleNonterminalDefsWithSameNameUse.fss",
//        "SyntaxExtends.fss",
//        "SyntaxExtendsUse.fss",
//        "SyntaxHelloWorldTemplate.fss",
//        "SyntaxHelloWorldTemplateUse.fss",
//        "SyntaxHelloWorld.fss",
//        "SyntaxHelloWorldUse.fss",
//        "SyntaxOption.fss",
//        "SyntaxOptionUse.fss",
//        "SyntaxSymbols.fss",
//        "SyntaxSymbolsUse.fss",
//        "SyntaxTemplateVars.fss",
//        "SyntaxTemplateVarsUse.fss",
//        "SyntaxTest.fss",
//        "SyntaxTestUse.fss",
//        "SXXGrammarExtendsNonExistingGrammarUse.fss",
//        "ImportEmptyApiWhichImportsNonEmptyApiEmpty.fss",
//        "ImportEmptyApiWhichImportsNonEmptyApiUse.fss",
//        "TemplateGapWithWrongASTType.fss",
//        "TemplateGapWithWrongASTTypeUse.fss",
//        "SXXTemplateGapWithInconsistentParametersUse.fss",
//        "ImportEmptyApiWhichImportsNonEmptyApiNonEmpty.fss");

    public static TestSuite suite() {
        TestCaseDir xml = new TestCaseDir(STATIC_TESTS_DIR+"/xml/", Collections.<String>emptyList(), null);
        @SuppressWarnings("unused")
		TestCaseDir regex = new TestCaseDir(STATIC_TESTS_DIR+"/regex/", Collections.<String>emptyList(), null);
        @SuppressWarnings("unused")
		TestCaseDir sql = new TestCaseDir(STATIC_TESTS_DIR+"/sql/", Collections.<String>emptyList(), null);
        TestCaseDir basisTests = new TestCaseDir(STATIC_TESTS_DIR, FAILING_SYNTAXABSTRACTIONS, null);

        StaticTestSuite suite = new StaticTestSuite("SyntaxAbstractionJUTestAll", basisTests);
        suite.addStaticTests(xml);
        // suite.addStaticTests(regex);
        // suite.addStaticTests(sql);
        return suite;
    }

}
