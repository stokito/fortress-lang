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

package com.sun.fortress.compiler.environments;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import junit.framework.TestCase;

import com.sun.fortress.Shell;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.interpreter.evaluator.BaseEnv;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.IntNat;
import com.sun.fortress.interpreter.evaluator.values.FInt;
import com.sun.fortress.useful.Path;

public class TopLevelEnvGenJUTest extends TestCase {

    private BaseEnv testCompiledEnv;
    private BaseEnv testCompiledImportEnv;
    private BaseEnv asciiValApiEnv;
 
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
    	String fssFiles[] = {"TestCompiledEnvironments", "TestCompiledImports"};
    	for(String fssFile : fssFiles) {
        	compileTestProgram(fssFile + ".fss");	
    	}
    	testCompiledEnv = loadEnvironment(fssFiles[0] + TopLevelEnvGen.COMPONENT_ENV_SUFFIX);
    	testCompiledImportEnv = loadEnvironment(fssFiles[1] + TopLevelEnvGen.COMPONENT_ENV_SUFFIX);
    	
    	String apiName = "AsciiVal";
       	asciiValApiEnv = loadEnvironment(apiName + TopLevelEnvGen.API_ENV_SUFFIX);
    }

    public void testNameMangling() {
    	String input = "/.;$<>[]:\\";
    	String mangledInput = "\\|\\,\\?\\%\\^\\_\\{\\}\\!\\-";
    	assertEquals(TopLevelEnvGen.mangleIdentifier(input), mangledInput);
    	input = "hello" + input;
    	mangledInput = "\\=" + "hello" + mangledInput;
    	assertEquals(TopLevelEnvGen.mangleIdentifier(input), mangledInput);
    }

    public void testRemoveMethods() {
        IntNat three = IntNat.make(3);
        testCompiledEnv.putTypeRaw("Empty", three);
        assertEquals(testCompiledEnv.getTypeNull("Empty"), three);
        testCompiledEnv.removeType("Empty");
        assertNull(testCompiledEnv.getTypeNull("Empty"));

        FInt seven = FInt.make(7);

        testCompiledEnv.putValueRaw("run", seven);
        assertEquals(testCompiledEnv.getValueRaw("run"), seven);
        testCompiledEnv.removeVar("run");
        assertNull(testCompiledEnv.getValueRaw("run"));

    }

    public void testNullGetters() {
        assertNull(testCompiledEnv.getBoolNull("run"));
        assertNull(testCompiledEnv.getIntNull("run"));
        assertNull(testCompiledEnv.getNatNull("run"));
    }

    public void testNullSetters() {
        testCompiledEnv.putBoolRaw("run", true);
        testCompiledEnv.putIntRaw("run", 0);
        testCompiledEnv.putNatRaw("run", 0);
    }
    
    public void testGetPutApi() {
    	String apiName = "AsciiVal";
    	FInt val = FInt.make(65);
    	
        assertNull(testCompiledImportEnv.getApiNull(apiName));        
        testCompiledImportEnv.putApi(apiName, asciiValApiEnv);
        Environment env = testCompiledImportEnv.getApiNull(apiName);        
        assertEquals(env, asciiValApiEnv);
        
        env.putValueRaw("A", val);
        assertEquals(asciiValApiEnv.getValueNull("A"), val);
    }
    
    public void testGetPutTypeRaw() {
        IntNat three = IntNat.make(3);
        IntNat a = IntNat.make((int) '$');
        IntNat b = IntNat.make((int) 'K');
        IntNat c = IntNat.make((int) 'l');

        testCompiledEnv.putTypeRaw("Empty", three);
        testCompiledEnv.putTypeRaw("Empty" + '\u05D0', a);
        testCompiledEnv.putTypeRaw("Empty" + '\u05D1', b);
        testCompiledEnv.putTypeRaw("Empty" + '\u05D2', c);

        assertEquals(testCompiledEnv.getTypeNull("Empty"), three);
        assertEquals(testCompiledEnv.getTypeNull("Empty" + '\u05D0'), a);
        assertEquals(testCompiledEnv.getTypeNull("Empty" + '\u05D1'), b);
        assertEquals(testCompiledEnv.getTypeNull("Empty" + '\u05D2'), c);

        assertNull(testCompiledEnv.getTypeNull("Chupacabra"));
    }

    public void testGetPutValueRaw() {

        FInt three = FInt.make(3);
        FInt seven = FInt.make(7);
        FInt thirteen = FInt.make(13);
        FInt a = FInt.make((int) '$');
        FInt b = FInt.make((int) 'K');
        FInt c = FInt.make((int) 'l');

        testCompiledEnv.putValueRaw("run", three);
        testCompiledEnv.putValueRaw("$", a);
        testCompiledEnv.putValueRaw("K", b);
        testCompiledEnv.putValueRaw("l", c);

        // Now test hash collisions
        testCompiledEnv.putValueRaw("Aa", seven);
        testCompiledEnv.putValueRaw("BB", thirteen);

        assertEquals(testCompiledEnv.getValueRaw("run"), three);
        assertEquals(testCompiledEnv.getValueRaw("$"), a);
        assertEquals(testCompiledEnv.getValueRaw("K"), b);
        assertEquals(testCompiledEnv.getValueRaw("l"), c);

        assertEquals(testCompiledEnv.getValueRaw("Aa"), seven);
        assertEquals(testCompiledEnv.getValueRaw("BB"), thirteen);
        assertNull(testCompiledEnv.getValueRaw("Chupacabra"));

    }

    public void testDump() throws IOException {
        FInt three = FInt.make(3);
        FInt a = FInt.make((int) '$');
        FInt b = FInt.make((int) 'K');
        FInt c = FInt.make((int) 'l');

        testCompiledEnv.putValueRaw("run", three);
        testCompiledEnv.putValueRaw("$", a);
        testCompiledEnv.putValueRaw("K", b);
        testCompiledEnv.putValueRaw("l", c);

    	StringBuffer buffer = new StringBuffer();
    	testCompiledEnv.verboseDump = true;
    	testCompiledEnv.dump(buffer);
    }

    private BaseEnv loadEnvironment(String className) throws IOException,
                                                             InstantiationException, 
                                                             IllegalAccessException {
        SimpleClassLoader classLoader = new SimpleClassLoader();
        File classfile = new File(ProjectProperties.BYTECODE_CACHE_DIR +
                                  File.separator + className + ".class");
        byte[] bytecode = new byte[(int) classfile.length()];
        FileInputStream classStream = new FileInputStream(classfile);
        int read = classStream.read(bytecode);
        if (read != classfile.length()) {
            fail("Expected to read " + classfile.length() + " bytes but read " + read + " bytes instead.");
        }

        Class generatedClass = classLoader.defineClass(className, bytecode);
        BaseEnv envObject = (BaseEnv) generatedClass.newInstance();
        return(envObject);
    }

    private void compileTestProgram(String testFileName) {
        Path path = ProjectProperties.SOURCE_PATH;
        String s = ProjectProperties.BASEDIR + "tests" +
                   File.separator + testFileName;

        File file = new File(s);
        s = file.getPath();

        if (s.contains(File.separator)) {
            String head = s.substring(0, s.lastIndexOf(File.separator));
            s = s.substring(s.lastIndexOf(File.separator)+1, s.length());
            path = path.prepend(head);
        }

        Iterable<? extends StaticError> errors = Shell.compile(path, s);

        for (StaticError error: errors) {
            fail(error.toString());
        }
    }
}
