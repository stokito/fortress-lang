/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.tools;

import com.sun.fortress.compiler.Parser;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.repository.ProjectProperties;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class AstJUTest extends TestCase {

    private static final char SEP = File.separatorChar;

    private String file;

    public AstJUTest(String file) {
        super(file);
        this.file = file;
    }

    @Override
    public void runTest() throws FileNotFoundException, IOException {
        File f = new File(file);
        assertEquals(parse(f), parse(unparse(parse(f))));
    }

    private Node parse(String buffer) throws IOException {
        return Parser.parseString(NodeFactory.makeAPIName(NodeFactory.testSpan, file), buffer);
    }

    private Node parse(File file) throws FileNotFoundException, IOException {
        return Parser.parseFileConvertExn(file);
    }

    private String unparse(Node node) {
        return node.accept(new FortressAstToConcrete());
    }

    private static Iterable<String> allTests(String dir) {
        String[] except = new String[]{
                "FortressSyntax.fsi", "XXXwrongName.fss", "FortressSyntax.fss"
        };
        final List<String> out = new LinkedList<String>(java.util.Arrays.asList(except));
        return Arrays.asList(new File(dir).list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (!out.contains(name) && (name.endsWith(".fss") || name.endsWith(".fsi")));
            }
        }));
    }

    private static void addSyntaxTests(TestSuite suite) {
        String syntax = ProjectProperties.FORTRESS_AUTOHOME + "/ProjectFortress/static_tests/syntax_abstraction";
        String[] tests = new String[]{
                "ChurchBooleans.fss", "Comprehension.fss", "ImportEmptyApiWhichImportsNonEmptyApiEmpty.fss",
                "ImportEmptyApiWhichImportsNonEmptyApiNonEmpty.fss", "SXXGrammarExtendsNonExistingGrammar.fss",
                "SXXTemplateGapWithInconsistentParameters.fss", "SXXTemplateParamsAreNotApplicable.fss",
                "SyntaxGrammarImports.fss", "SyntaxGrammarImportsA.fss", "SyntaxNodes.fss",
                "TemplateGapWithWrongASTType.fss", "UsingJavaIdentifiersAsPatternVariables.fss", "ChurchBooleans.fsi",
                "Comprehension.fsi", "ImportEmptyApiWhichImportsNonEmptyApiEmpty.fsi",
                "ImportEmptyApiWhichImportsNonEmptyApiNonEmpty.fsi", "SXXGrammarExtendsNonExistingGrammar.fsi",
                "SXXTemplateGapWithInconsistentParameters.fsi", "SXXTemplateParamsAreNotApplicable.fsi",
                "SyntaxGrammarImports.fsi", "SyntaxGrammarImportsA.fsi", "SyntaxNodes.fsi",
                "TemplateGapWithWrongASTType.fsi", "UsingJavaIdentifiersAsPatternVariables.fsi"
        };

        for (String file : tests) {
            suite.addTest(new AstJUTest(syntax + SEP + file));
        }
    }

    public static Test suite() throws IOException {
        String tests = ProjectProperties.FORTRESS_AUTOHOME + "/ProjectFortress/tests";
        String library = ProjectProperties.FORTRESS_AUTOHOME + "/Library";
        String demos = ProjectProperties.FORTRESS_AUTOHOME + "/ProjectFortress/demos";
        String builtin = ProjectProperties.FORTRESS_AUTOHOME + "/ProjectFortress/LibraryBuiltin";
        /* we can't add all the syntax tests because some of the blahUse.fss files
        * use the macros, so they have non-standard syntax in the first place.
        */
        //String syntax = ProjectProperties.FORTRESS_AUTOHOME + "/ProjectFortress/static_tests/syntax_abstraction";
        TestSuite suite = new TestSuite("Parses all .fss and .fsi files.");
        String[] dirs = new String[]{tests, library, demos, builtin};
        for (String dir : dirs) {
            for (String file : allTests(dir)) {
                suite.addTest(new AstJUTest(dir + SEP + file));
            }
        }
        addSyntaxTests(suite);
        return suite;
    }
}
