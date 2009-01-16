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
import java.io.FilenameFilter;
import java.util.Collections;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.compiler.StaticTestSuite;
import com.sun.fortress.compiler.StaticTestSuite.TestCaseDir;
import com.sun.fortress.repository.ProjectProperties;

public class SyntaxAbstractionJUTestAll extends TestCase {
    private final static String STATIC_TESTS_DIR =
        ProjectProperties.BASEDIR + "static_tests/syntax_abstraction/";

    public static TestSuite suite() {
        FilenameFilter fssFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith("fss");
            }
        };
        String[] basisTests = new File(STATIC_TESTS_DIR).list(fssFilter);
        TestSuite suite = new TestSuite("SyntaxAbstractionJUTestAll");
        for ( String filename : basisTests ){
            File f = new File(STATIC_TESTS_DIR + filename);
            suite.addTest(new StaticTestSuite.StaticTestCase(f, false));
        }

        @SuppressWarnings("unused")
        TestCaseDir xml = new TestCaseDir(STATIC_TESTS_DIR+"xml/", Collections.<String>emptyList(), null);
        @SuppressWarnings("unused")
        TestCaseDir regex = new TestCaseDir(STATIC_TESTS_DIR+"regex/", Collections.<String>emptyList(), null);
        @SuppressWarnings("unused")
        TestCaseDir sql = new TestCaseDir(STATIC_TESTS_DIR+"sql/", Collections.<String>emptyList(), null);
        // suite.addStaticTests(xml);
        // suite.addStaticTests(regex);
        // suite.addStaticTests(sql);
        return suite;
    }
}

/*
not_yet_passing/
regex/
sql/
transformer/
xml/

syntax_abstraction 306> l -1 not_yet_passing/
SXXTemplateGapWithInconsistentParameters.fsi
SXXTemplateGapWithInconsistentParameters.fss
SXXTemplateGapWithInconsistentParametersUse.fss
SXXTemplateParamsAreNotApplicable.fsi
SXXTemplateParamsAreNotApplicable.fss
SXXTemplateParamsAreNotApplicableUse.fss
TemplateGapWithWrongASTType.fsi
TemplateGapWithWrongASTType.fss
TemplateGapWithWrongASTTypeUse.fss
UsingJavaIdentifiersAsPatternVariables.fsi
UsingJavaIdentifiersAsPatternVariables.fss
UsingJavaIdentifiersAsPatternVariablesUse.fss

syntax_abstraction 307> l -1 regex/
Regex.fsi
Regex.fss
RegexUse1.fss
RegexUse2.fss

syntax_abstraction 308> l -1 sql
Sql.fsi
Sql.fss
SqlUse.fss

syntax_abstraction 309> l -1 transformer/
SXXMultipleGrammarsWithSameName.fsi
SXXMultipleGrammarsWithSameName.fss
SXXMultipleGrammarsWithSameNameUse.fss
SXXSyntaxMultipleNonterminalDefsWithSameName.fsi
SXXSyntaxMultipleNonterminalDefsWithSameName.fss
SXXSyntaxMultipleNonterminalDefsWithSameNameUse.fss
SyntaxAST.fsi
SyntaxAST.fss
SyntaxASTUse.fss
SyntaxExtends.fsi
SyntaxExtends.fss
SyntaxExtendsUse.fss
SyntaxHelloWorld.fsi
SyntaxHelloWorld.fss
SyntaxHelloWorldTemplate.fsi
SyntaxHelloWorldTemplate.fss
SyntaxHelloWorldTemplateUse.fss
SyntaxHelloWorldUse.fss
SyntaxOption.fsi
SyntaxOption.fss
SyntaxOptionUse.fss
SyntaxRepetition.fsi
SyntaxRepetition.fss
SyntaxRepetitionUse.fss
SyntaxSymbols.fsi
SyntaxSymbols.fss
SyntaxSymbolsUse.fss
SyntaxTemplateVars.fsi
SyntaxTemplateVars.fss
SyntaxTemplateVarsUse.fss
SyntaxTest.fsi
SyntaxTest.fss
SyntaxTestUse.fss

syntax_abstraction 310> l -1 xml
Xml.fsi
Xml.fss
XmlUse.fss
*/
