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

package com.sun.fortress.compiler;

import junit.framework.TestCase;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Collections;
import java.util.Arrays;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.io.IOUtil;
import edu.rice.cs.plt.lambda.Predicate;
import edu.rice.cs.plt.lambda.Lambda;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.DottedName;
import com.sun.fortress.shell.FileBasedRepository;

import com.sun.fortress.interpreter.drivers.ProjectProperties;
public class CompilerTopLevelJUTest extends TestCase {

    private static final boolean VERBOSE = false;
    private static final boolean SKIP_NOT_PASSING = true;

    // relative to the top ProjectFortress directory
    private static String baseDir = ProjectProperties.BASEDIR;
    private static String staticTests = baseDir + "static_tests/";

    private static final List<String> NOT_PASSING = Arrays.asList(
        staticTests + "XXXMultipleRefErrors.fss",
        staticTests + "XXXUndefinedArrayRef.fss",
        staticTests + "XXXUndefinedInitializer.fss",
        staticTests + "XXXUndefinedNestedRef.fss",
        staticTests + "XXXUndefinedRefInLoop.fss",
        staticTests + "XXXUndefinedVar.fss",
        staticTests + "XXXUndefinedTopLevelVar.fss",
        /* Tests for Syntax abstractions */
        staticTests + "SyntaxHelloWorldUse.fss",
        staticTests + "SyntaxProductionExtends.fsi",
        staticTests + "XXXSyntaxMultipleGrammarsWithSameName.fsi",
        staticTests + "XXXSyntaxMultipleProductionsWithSameName.fsi",
        staticTests + "XXXSyntaxGrammarExtendsNonExistingGrammar.fsi",
        // really not working:
        staticTests + "XXXSyntaxNoFortressAstImport.fsi",
        staticTests + "stub to eliminate comma trouble"
    );

    private static final Set<File> NOT_PASSING_FILES =
      CollectUtil.asSet(IterUtil.map(NOT_PASSING, new Lambda<String, File>() {
        public File value(String s) { return new File(s); }
      }));

    public void testStaticTests() throws IOException {
        boolean foundAFile = false;
        Predicate<File> filter = IOUtil.extensionFilePredicate("fss", IOUtil.IS_FILE);
        for (File f : IOUtil.listFilesRecursively(new File(staticTests), filter)) {
            foundAFile = true;
            if (SKIP_NOT_PASSING && NOT_PASSING_FILES.contains(f)) { continue; }
            else if (f.getName().contains("XXX")) { assertMalformedProgram(f); }
            else { assertWellFormedProgram(f); }
        }
        assertTrue("No test files found in the static_tests directory", foundAFile);
    }

    private void assertMalformedProgram(File f) throws IOException {
        Iterable<? extends StaticError> errors = compile(f);
        assertFalse("Source " + f + " was compiled without error",
                    IterUtil.isEmpty(errors));
        if (VERBOSE) {
            System.out.println(f + "  OK -- errors:");
            System.out.println(IterUtil.multilineToString(errors));
        }
    }

    private void assertWellFormedProgram(File f) throws IOException {
        Iterable<? extends StaticError> errors = compile(f);
        String message = "Source " + f + " produces static errors:\n" +
                         IterUtil.multilineToString(errors);
        assertTrue(message, IterUtil.isEmpty(errors));
        if (VERBOSE) { System.out.println(f + "  OK"); }
    }

    private Iterable<? extends StaticError> compile(File f) throws IOException {
        final Map<DottedName, ApiIndex> apis = new HashMap<DottedName, ApiIndex>();
        Fortress fortress = new Fortress(new FileBasedRepository(baseDir, staticTests + "lib"));
//        new FortressRepository() {
//            public Map<DottedName, ApiIndex> apis() {
//                return Collections.unmodifiableMap(apis);
//            }
//            public void addApi(DottedName name, ApiIndex def) {
//                apis.put(name, def);
//            }
//            public void addComponent(DottedName name, ComponentIndex def) {
//                /* ignore */
//            }
//        });
        return fortress.compile(f);
    }

}
