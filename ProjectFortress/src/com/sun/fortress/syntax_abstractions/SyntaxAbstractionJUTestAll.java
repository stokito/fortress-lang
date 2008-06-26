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
import java.util.LinkedList;
import java.util.List;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.compiler.StaticTestSuite;
import com.sun.fortress.interpreter.drivers.ProjectProperties;

public class SyntaxAbstractionJUTestAll extends TestCase {
    
    private final static String STATIC_TESTS_DIR = ProjectProperties.BASEDIR + "static_tests/syntax_abstraction/";
    private final static List<String> FAILING_SYNTAXABSTRACTIONS = Arrays.asList(
        "SyntaxASTUse.fss",
        "SyntaxAST.fss",
        "ChurchBooleansUse.fss",
        "ChurchBooleans.fss",
        "ChurchBooleans.fsi",
        "XXXSyntaxGrammarImportsUse.fss",
        "SXXTemplateGapWithWrongASTTypeUse.fss",
        "ImportApiEmptyApiWhichImportsNonEmptyApiUse.fss");
        
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
        StaticTestSuite suite = new StaticTestSuite("SyntaxAbstractionJUTest",
                STATIC_TESTS_DIR,
                FAILING_SYNTAXABSTRACTIONS,
                null);
//        StaticTestSuite xml = new StaticTestSuite("SyntaxAbstraction Xml Test",
//                STATIC_TESTS_DIR+"/xml",
//                FAILING_SYNTAXABSTRACTIONS,
//                null);
//        StaticTestSuite regexp = new StaticTestSuite("SyntaxAbstraction RegExp Test",
//                STATIC_TESTS_DIR,
//                FAILING_SYNTAXABSTRACTIONS,
//                null);
        return suite;
    }
    
}
