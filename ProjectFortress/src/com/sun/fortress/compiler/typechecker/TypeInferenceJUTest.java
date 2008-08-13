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

package com.sun.fortress.compiler.typechecker;

import static edu.rice.cs.plt.debug.DebugUtil.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.fortress.Shell;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.repository.ProjectProperties;

public class TypeInferenceJUTest extends TestCase {
    public static void main(String... args) {
        junit.textui.TestRunner.run(TypeAnalyzerJUTest.class);
    }

    public void testConstraints(){
    	debug.logStart();
    	try {
    	/*
    	TypeAnalyzer t = makeAnalyzer(trait("Int"));
    	_InferenceVarType i1=NodeFactory.make_InferenceVarType(new Span());
    	_InferenceVarType i2=NodeFactory.make_InferenceVarType(new Span());
    	_InferenceVarType i3=NodeFactory.make_InferenceVarType(new Span());
    	_InferenceVarType i4=NodeFactory.make_InferenceVarType(new Span());
    	ConstraintFormula t1 = upperBound(i1,i2,t.new SubtypeHistory()).and(
    			lowerBound(i1,type("Int"),t.new SubtypeHistory()),t.new SubtypeHistory()).and(
    					upperBound(i2,type("Int"),t.new SubtypeHistory()), t.new SubtypeHistory()).and(
    							upperBound(i2,i1,t.new SubtypeHistory()),t.new SubtypeHistory());
    	assertTrue(t1.isSatisfiable());


    	t = makeAnalyzer(trait("A"),trait("B"),trait("C"),trait("D","C"));
    	ConstraintFormula t2 =  upperBound(i1,type("A"),t.new SubtypeHistory()).and(lowerBound(i1,type("A"),t.new SubtypeHistory()),t.new SubtypeHistory());
    	ConstraintFormula t3 =  t2.and(upperBound(i2,type("B"),t.new SubtypeHistory()).and(lowerBound(i2,type("B"),t.new SubtypeHistory()),t.new SubtypeHistory()),t.new SubtypeHistory());
    	ConstraintFormula t4 =  t3.and(upperBound(i3,type("C"),t.new SubtypeHistory()).and(lowerBound(i3,type("C"),t.new SubtypeHistory()),t.new SubtypeHistory()),t.new SubtypeHistory());
    	ConstraintFormula t5 =  t4.and(upperBound(i4,type("D"),t.new SubtypeHistory()).and(lowerBound(i4,type("D"),t.new SubtypeHistory()),t.new SubtypeHistory()),t.new SubtypeHistory());
    	ConstraintFormula t6 = t5.and(t.subtype(NodeFactory.makeIntersectionType(i1, i4),NodeFactory.makeUnionType(i2, i3)),t.new SubtypeHistory());
    	assertTrue(t6.isSatisfiable());*/
    	} finally { debug.logEnd(); }
    }

    public static Test suite() throws IOException, Throwable {
        TestSuite suite = new TestSuite("Tests type inference." );

        // Add every inference test that we'd like to have

        // Test static param inference
        suite.addTest(StaticArgInferenceTest.suite());

        return suite;
    }

    /**
     * Test the inference of static arguments at a call site.
     */
    static class StaticArgInferenceTest extends TestCase {
        private static final char SEP = File.separatorChar;
        private static final String STATIC_ARG_INF_TEST_DIR = ProjectProperties.FORTRESS_AUTOHOME + SEP + "ProjectFortress" + SEP + "static_tests" + SEP + "static_arg_inference" + SEP;
        private static final String CACHED_TFS_NAME = "In.tfs";
        private static final String OUT_FILE_NAME = "Out.fss";
        private static final String EXPECTED_FILE_NAME = "Expected.fss";

        private File testDirectory;

        public StaticArgInferenceTest(File test_dir) {
            super(test_dir.getAbsolutePath());
            this.testDirectory = test_dir;
        }

        public static Test suite() throws IOException, Throwable {
            TestSuite suite = new TestSuite("Tests inference of static arguments at call sites.");

            File top_level_dir = new File(STATIC_ARG_INF_TEST_DIR);

            File[] test_dirs =
                top_level_dir.listFiles(new FileFilter(){
                    public boolean accept(File arg0) { return arg0.isDirectory() && !arg0.isHidden(); }});

            for( File test_dir : test_dirs ) {
                suite.addTest(new StaticArgInferenceTest(test_dir));
            }

            return suite;
        }

        @Override
        protected void runTest() throws Throwable {
            String[] infiles =testDirectory.list(new FilenameFilter(){
                public boolean accept(File arg0, String arg1) {
                    return arg1.startsWith("In")&&arg1.endsWith(".fss");
                }
                
            });
            String fq_input_fname = testDirectory.getAbsolutePath() + SEP + infiles[0];
            String fq_output_fname = testDirectory.getAbsolutePath() + SEP + OUT_FILE_NAME;
            String fq_exp_fname = testDirectory.getAbsolutePath() + SEP + EXPECTED_FILE_NAME;
            String fq_cached_fname = testDirectory.getAbsolutePath() + SEP + CACHED_TFS_NAME;

            String[] command = new String[] {"typecheck", "-out", fq_cached_fname, fq_input_fname};
            Shell.main(command);

            command = new String[]{"unparse", "-out", fq_output_fname, fq_cached_fname};
            Shell.main(command);

            ASTIO.deleteJavaAst(fq_cached_fname);

            assertSameFile(fq_output_fname, fq_exp_fname);
        }

        private void assertSameFile(String f_1, String f_2) throws FileNotFoundException, IOException {
            int line_number = 1;

            BufferedReader f_1_reader = new BufferedReader(new FileReader(f_1));
            BufferedReader f_2_reader = new BufferedReader(new FileReader(f_2));

            String f_1_line = f_1_reader.readLine();
            String f_2_line = f_2_reader.readLine();
            while( f_1_line != null && f_2_line != null ) {
                assertEquals("File " +f_1+" and file " + f_2 + " were not the same, line " + line_number, f_1_line, f_2_line);

                f_1_line = f_1_reader.readLine();
                f_2_line = f_2_reader.readLine();
                line_number ++;
            }

            if(f_1_line != null) {
                assertTrue("File " + f_1 + " is longer than " + f_2 + ", starting at line " + line_number, false);
            }
            else if( f_2_line != null ) {
                assertTrue("File " + f_2 + " is longer than " + f_1 + ", starting at line " + line_number, false);
            }
        }
    }
}
