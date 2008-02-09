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

import junit.framework.TestCase;
import java.io.File;
import java.io.FilenameFilter;
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
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.shell.CacheBasedRepository;
import com.sun.fortress.shell.FileBasedRepository;

import com.sun.fortress.interpreter.drivers.ProjectProperties;
public abstract class StaticTest extends TestCase {

    private final boolean VERBOSE = true;
    private final boolean SKIP_NOT_PASSING = true;

    // relative to the top ProjectFortress directory
    protected String baseDir = ProjectProperties.BASEDIR;
    protected String staticTests = baseDir + "static_tests/";
    
    public abstract List<String> getNotPassing();
    
    public void testStaticTests() throws IOException {
        Set<File> notPassingFiles = 
        CollectUtil.asSet(IterUtil.map(getNotPassing(), new Lambda<String, File>() {
            public File value(String s) { return new File(s); }
        }));
        boolean foundAFile = false;
        for (String filename : new File(staticTests).list(new FilenameFilter() {
        		public boolean accept(File dir, String name) {
        			return name.endsWith("fss");
        		}})) {
        	File f = new File(staticTests+filename);
            foundAFile = true;
            if (SKIP_NOT_PASSING && notPassingFiles.contains(f)) { continue; }
            else {
            	System.out.println("Testing: "+f.getName());
            	if (f.getName().contains("XXX")) { assertMalformedProgram(f); }
            	else { assertWellFormedProgram(f); }
            }
            
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
        //Fortress fortress = new Fortress(new FileBasedRepository(baseDir, staticTests + "lib"));
        Fortress fortress = new Fortress(new CacheBasedRepository(ProjectProperties.ANALYZED_CACHE_DIR));
        return fortress.compile(ProjectProperties.SOURCE_PATH.prepend(f.getParent()), f.getName());
    }

}
