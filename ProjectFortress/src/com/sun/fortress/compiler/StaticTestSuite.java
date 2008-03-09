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

package com.sun.fortress.compiler;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.io.IOUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Predicate;
import edu.rice.cs.plt.lambda.Lambda;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.interpreter.drivers.ProjectProperties;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.shell.CacheBasedRepository;
import com.sun.fortress.shell.FileBasedRepository;

public final class StaticTestSuite extends TestSuite {
    
    private final static boolean VERBOSE = false;
    private final static boolean SKIP_FAILING = true;
    
    // relative to the top ProjectFortress directory
    private final String testDir;
    private final Set<File> failingDisambiguator;
    private final Set<File> failingTypeChecker;
    private final boolean skipTypeChecker;

    public StaticTestSuite(String _name, String _testDir, List<String> _failingDisambiguator, List<String> _failingTypeChecker) {
        super(_name);
        testDir = _testDir;
        skipTypeChecker = (_failingTypeChecker == null);
        failingDisambiguator = fileSetFromStringList(_failingDisambiguator);
        failingTypeChecker = fileSetFromStringList(_failingTypeChecker);
        addStaticTests();
    }
    
    private void addStaticTests() {
        FilenameFilter fssFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith("fss");
            }
        };
        
        for (String filename : new File(testDir).list(fssFilter)) {
            File f = new File(testDir + filename);
            
            if (SKIP_FAILING && isFailingDisambiguator(f)) {
                // skip
            } else if (skipTypeChecker || (SKIP_FAILING && isFailingTypeChecker(f))) {
                addTest(new StaticTestCase(f, false));
            } else {
                addTest(new StaticTestCase(f));
            }
        }
    }
    
    private Set<File> fileSetFromStringList(List<String> files) {
        if (files == null) {
            return CollectUtil.<File>emptySet();
        } else {
            return CollectUtil.asSet(IterUtil.map(files, new Lambda<String, File>() {
                public File value(String s) { return new File(testDir + s); }
            }));
        }
    }
    
    protected boolean isFailingDisambiguator(File f) {
        return failingDisambiguator.contains(f);
    }
    
    protected boolean isFailingTypeChecker(File f) {
        return failingDisambiguator.contains(f) || failingTypeChecker.contains(f);
    }
    
    public static class StaticTestCase extends TestCase {
        
        private final File file;
        private final boolean typecheck;
        private boolean typecheckStore = true;
        
        /**
         * Construct a static test case for the given file.  The file will be type checked
         * as dictated by the file name.
         */
        public StaticTestCase(File _file) {
            super(_file.getName());
            file = _file;
            typecheck = true;
        }
        
        /**
         * Construct a static test case for the given file.  The typecheck parameter should
         * be false if the test should not be type checked (i.e. if the file name dictates
         * that it should be, but it is not currently passing type checking).
         */
        public StaticTestCase(File _file, boolean _typecheck) {
            super(_file.getName() + (_typecheck ? "" : " (type checking disabled)"));
            file = _file;
            typecheck = _typecheck;
        }
        
        @Override
        protected void setUp() throws Exception {
            typecheckStore = com.sun.fortress.compiler.StaticChecker.typecheck;
            com.sun.fortress.compiler.StaticChecker.typecheck = typecheck;
        }
        
        @Override
        protected void tearDown() throws Exception {
            com.sun.fortress.compiler.StaticChecker.typecheck = typecheckStore;
        }
        
        @Override
        protected void runTest() throws Throwable {
            if (file.getName().startsWith("XXX")) {
                assertDisambiguatorFails(file);
            } else if (file.getName().startsWith("DXX") && typecheck) {
                assertTypeCheckerFails(file);
            } else {
                assertWellFormedProgram(file);
            }
        }
        
        private void assertDisambiguatorFails(File f) throws IOException {
            com.sun.fortress.compiler.StaticChecker.typecheck = false;
            Iterable<? extends StaticError> errors = compile(f);
            assertFalse("Source " + f + " was compiled without disambiguator errors",
                        IterUtil.isEmpty(errors));
            if (VERBOSE) {
                System.out.println(f + "  OK -- errors:");
                System.out.println(IterUtil.multilineToString(errors));
            }
        }
        
        private void assertTypeCheckerFails(File f) throws IOException {
            Iterable<? extends StaticError> allErrors = compile(f);
            String message = "";
            if (!IterUtil.isEmpty(allErrors)) {
            	message = " but got:";
            }
            List<TypeError> typeErrors = new ArrayList<TypeError>();
            for (StaticError error : allErrors) {
                try { throw error; }
                catch (TypeError e) { typeErrors.add(e); }
                catch (Fortress.WrappedException e) {
                    e.getCause().printStackTrace();
                    message += "\nStaticError (wrapped): " + e.getCause().toString();
                }
                catch (StaticError e) { message += "\nStaticError: " + e.toString(); }
            }
            assertFalse("Source " + f + " was compiled without TypeErrors" + message,
                        IterUtil.isEmpty(typeErrors));
            if (VERBOSE) {
                System.out.println(f + "  OK -- errors:");
                System.out.println(IterUtil.multilineToString(typeErrors));
            }
        }
        
        private void assertWellFormedProgram(File f) throws IOException {
            Iterable<? extends StaticError> errors = compile(f);
            String message = "Source " + f + " produces static errors:";
            for (StaticError error : errors) {
                try { throw error; }
                catch (Fortress.WrappedException e) {
                    e.getCause().printStackTrace();
                    message += "\nStaticError (wrapped): " + e.getCause().toString();
                }
                catch (StaticError e) { message += "\nStaticError: " + e.toString(); }
            }
            assertTrue(message, IterUtil.isEmpty(errors));
            if (VERBOSE) { System.out.println(f + "  OK"); }
        }
        
        private Iterable<? extends StaticError> compile(File f) throws IOException {
            //Fortress fortress = new Fortress(new FileBasedRepository(baseDir, staticTests + "lib"));
            Fortress fortress = new Fortress(new CacheBasedRepository(ProjectProperties.ANALYZED_CACHE_DIR));
            return fortress.compile(ProjectProperties.SOURCE_PATH.prepend(f.getParent()), f.getName());
        }
    }
}
