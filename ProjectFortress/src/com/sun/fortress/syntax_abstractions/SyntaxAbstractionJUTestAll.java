/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions;

import com.sun.fortress.Shell;
import com.sun.fortress.compiler.phases.PhaseOrder;

import com.sun.fortress.compiler.StaticTestSuite;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.tests.unit_tests.FileTests;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class SyntaxAbstractionJUTestAll extends TestCase {

    private static final char SEP = File.separatorChar;
    private final static String STATIC_TESTS_DIR = ProjectProperties.BASEDIR + "syntax_abstraction_tests";

    public static TestSuite suite() throws IOException {
        Shell.setPhaseOrder(PhaseOrder.interpreterPhaseOrder);
        FilenameFilter fssFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith("Use.fss");
            }
        };
        String[] basisTests = new File(STATIC_TESTS_DIR).list(fssFilter);
        TestSuite suite = new TestSuite("SyntaxAbstractionJUTestAll");
        for (String filename : basisTests) {
            if (filename.contains("SXX")) {
                assertFails(suite, filename);
            } else {
                String testname = filename.substring(0, filename.lastIndexOf(".fss"));
                assertSucceeds(suite, testname);
            }
        }
        return suite;
    }

    private static void assertFails(TestSuite suite, String filename) {
        File f = new File(STATIC_TESTS_DIR + SEP + filename);
        suite.addTest(new StaticTestSuite.StaticTestCase(f, false));
    }

    private static void assertSucceeds(TestSuite suite, String testname) throws IOException {
        suite.addTest(new FileTests.InterpreterTest(STATIC_TESTS_DIR, STATIC_TESTS_DIR, testname, true, false));
    }
}

/*
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

syntax_abstraction 308> l -1 sql
Sql.fsi
Sql.fss
SqlUse.fss
*/
